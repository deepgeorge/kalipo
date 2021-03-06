package org.kalipo.service;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.kalipo.aop.KalipoExceptionHandler;
import org.kalipo.aop.RateLimit;
import org.kalipo.config.Constants;
import org.kalipo.config.ErrorCode;
import org.kalipo.domain.Comment;
import org.kalipo.domain.Report;
import org.kalipo.repository.CommentRepository;
import org.kalipo.repository.ReportRepository;
import org.kalipo.security.Privileges;
import org.kalipo.security.SecurityUtils;
import org.kalipo.service.util.Asserts;
import org.kalipo.service.util.NumUtils;
import org.kalipo.web.rest.KalipoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import javax.annotation.security.RolesAllowed;
import javax.inject.Inject;
import java.util.concurrent.Future;

/**
 * on approve: reputation + 5 for every reporter, reputation - 100 for author
 * on reject: reputation - 50 for author
 * on report: iff report count > 5 comment will become hidden until reviewed
 */
@Service
@KalipoExceptionHandler
public class ReportService {

    public static final int CRITICAL_REPORT_COUNT = 3;
    private final Logger log = LoggerFactory.getLogger(ReportService.class);

    @Inject
    private ReportRepository reportRepository;

    @Inject
    private CommentRepository commentRepository;

    @Inject
    private ReputationModifierService reputationModifierService;

    @Inject
    private CommentService commentService;

    @Inject
    private NotificationService notificationService;

    //    @RolesAllowed(AuthoritiesConstants.ANONYMOUS)
    @RateLimit
    public Report create(Report report) throws KalipoException {

        validate(report);

        final String author = SecurityUtils.getCurrentLogin();

        report.setAuthorId(author);

        Comment comment = commentRepository.findOne(report.getCommentId());

        boolean isDuplicate = reportRepository.findByCommentIdAndAuthorId(comment.getId(), author) != null;

        return _create(report, comment, author, isDuplicate);
    }

    private void validate(Report report) throws KalipoException {
        Asserts.isNotNull(report, "report");
        Asserts.isNull(report.getId(), "id");
        Asserts.isNotNull(report.getCommentId(), "commentId");
    }

    @RateLimit
    public Report createAnonymous(Report report) throws KalipoException {

        validate(report);

        String email = report.getEmail();
        Asserts.isNotNull(report.getEmail(), "email");
        Asserts.isNotNull(report.getIp(), "ip");

        report.setEmail(email);

        Comment comment = commentRepository.findOne(report.getCommentId());
        boolean isDuplicate = StringUtils.isNotBlank(email) && reportRepository.findByCommentIdAndEmail(comment.getId(), email) != null;

        return _create(report, comment, email, isDuplicate);
    }

    private Report _create(Report report, Comment comment, String author, boolean isDuplicate) throws KalipoException {

        if (isDuplicate) {
            throw new KalipoException(ErrorCode.CONSTRAINT_VIOLATED, "You already filed a report for this comment");
        }

        if (Report.Reason.Other == report.getReason()) {
            Asserts.isNotNull(report.getCustomReason(), "customReason");
            log.info(String.format("User '%s' reports comment %s saying %s", author, comment.getId(), report.getCustomReason()));
        } else {
            log.info(String.format("User '%s' reports comment %s saying %s", author, comment.getId(), report.getReason()));
        }

        // todo if anon report, approve via email?

        report.setStatus(Report.Status.PENDING);
        report.setThreadId(comment.getThreadId());
        report.setCommentId(comment.getId());

        report = reportRepository.save(report);

        // todo reject reports on already manually approved comments
        Integer reportedCount = NumUtils.nullToZero(comment.getReportedCount()) + 1;
        comment.setReportedCount(reportedCount);

        if (reportedCount == 1) {
            notificationService.announcePendingReport(comment.getThreadId(), report);
        }
        if (reportedCount == CRITICAL_REPORT_COUNT) {
            log.info(String.format("Hiding comment %s after %s reports", comment.getId(), CRITICAL_REPORT_COUNT));
            comment.setHidden(true);
            notificationService.notifySuperModsOfFraudulentComment(comment, author);
        }
        commentRepository.save(comment);

        return report;
    }

    @RolesAllowed(Privileges.CLOSE_REPORT)
    @RateLimit
    public void approve(String id) throws KalipoException {
        approveOrRejectReport(getPendingReport(id).setStatus(Report.Status.APPROVED));
    }

    @RolesAllowed(Privileges.CLOSE_REPORT)
    @RateLimit
    public void reject(String id) throws KalipoException {
        approveOrRejectReport(getPendingReport(id).setStatus(Report.Status.REJECTED));
    }

    // todo add RolesAllowed
    @Async
    public Future<Page<Report>> filtered(Report.Status status, int page) {
        PageRequest pageable = new PageRequest(page, Constants.PAGE_SIZE, Sort.Direction.DESC, Constants.PARAM_CREATED_DATE);
        return new AsyncResult<>(reportRepository.findByStatus(status, pageable));
    }

    @Async
    public Future<Report> get(String id) throws KalipoException {
        return new AsyncResult<Report>(reportRepository.findOne(id));
    }

    /**
     * Delete a pending report
     *
     * @param id the report id
     * @throws org.kalipo.web.rest.KalipoException
     */
    // todo add RolesAllowed
    public void delete(String id) throws KalipoException {
        // todo delete replaces reject
        getPendingReport(id); // will fail if not pending or existing

        reportRepository.delete(id);
    }

    // --

    // todo delete replaces reject
    private void approveOrRejectReport(Report report) throws KalipoException {

        final String currentLogin = SecurityUtils.getCurrentLogin();

        report.setReviewerId(currentLogin);

        Comment comment = commentRepository.findOne(report.getCommentId());

        Asserts.isNotNull(comment, "comment");

        if (report.getStatus() == Report.Status.APPROVED) {
            log.info(String.format("%s approves report %s and triggers delete-comment", currentLogin, report.getId()));
            commentService.delete(comment);
            notificationService.announceReportApproved(report);

        } else {

            if (BooleanUtils.isTrue(comment.getHidden())) {
                log.info(String.format("%s rejects report %s - comment is visible again", currentLogin, report.getId()));
                comment.setHidden(false);
                commentRepository.save(comment);
            } else {
                log.info(String.format("%s rejects report %s", currentLogin, report.getId()));
            }

            notificationService.announceReportRejected(report);
        }

        reputationModifierService.onReportApprovalOrRejection(reportRepository.save(report));
    }

    private Report getPendingReport(String id) throws KalipoException {
        Report report = reportRepository.findOne(id);

        Asserts.isNotNull(report, "id");

        if (report.getStatus() != Report.Status.PENDING) {
            throw new KalipoException(ErrorCode.CONSTRAINT_VIOLATED, "Report must be pending");
        }
        return report;
    }
}
