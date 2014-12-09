package org.kalipo.service;

import org.kalipo.domain.*;
import org.kalipo.domain.Thread;
import org.kalipo.repository.*;
import org.kalipo.security.SecurityUtils;
import org.kalipo.service.util.Asserts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This service is used to write notification messages about likes (LIKE), mentions in a comment (MENTION), replies (REPLY), 3rd party comment deletion (DELETION), reports of comments to mods and supermods (REPORT), REVIEW, APPROVAL
 */
@Service
public class NoticeService {

    private static final int PAGE_SIZE = 20;
    private static final Pattern FIND_USER_REFERENCES = Pattern.compile("@[a-z0-9]+"); //todo fix pattern to not match email addresses

    private final Logger log = LoggerFactory.getLogger(NoticeService.class);

    @Inject
    private NoticeRepository noticeRepository;

    @Inject
    private ThreadRepository threadRepository;

    @Inject
    private CommentRepository commentRepository;

    @Inject
    private UserRepository userRepository;

    @Inject
    private UserRepositoryCustom userRepositoryCustom;

    public List<Notice> findByUser(final String login, final int pageNumber) {
        PageRequest pageable = new PageRequest(pageNumber, PAGE_SIZE, Sort.Direction.DESC, "createdDate");
        return noticeRepository.findByRecipientId(login, pageable);
    }

    // -- ASYNCHRONOUS CALLS -------------------------------------------------------------------------------------------

    @Async
    public void notifyMentionedUsers(Comment comment) {
        try {
            Asserts.isNotNull(comment, "comment");

            if (comment.getStatus() == Comment.Status.APPROVED) {
                // find mentioned usernames, starting with @ like @myname
                Matcher matcher = FIND_USER_REFERENCES.matcher(comment.getText());
                Set<String> uqLogins = new HashSet<String>();
                while (matcher.find()) {
                    String login = matcher.group();
                    uqLogins.add(login);
                }

                for (String login : uqLogins) {
                    // notify @login
                    sendNotice(login, Notice.Type.MENTION, comment.getId());
                }
            }
        } catch (Exception e) {
            log.error(String.format("Unable to notify mentioned user. Reason: %s", e.getMessage()));
        }
    }

    @Async
    public void notifyModsOfThread(String threadId, Report report) {
        try {
            Asserts.isNotNull(threadId, "threadId");
            Asserts.isNotNull(report, "report");

            Thread thread = threadRepository.findOne(threadId);
            Asserts.isNotNull(thread, "threadId");

            thread.getModIds().forEach(modId -> sendNotice(modId, Notice.Type.REPORT, report.getCommentId()));

        } catch (Exception e) {
            log.error(String.format("Unable to notify mods of thread %s with report %s. Reason: %s", threadId, report, e.getMessage()));
        }
    }

    @Async
    public void notifyAuthorOfParent(Comment comment) {
        try {
            Asserts.isNotNull(comment, "comment");

            if (comment.getParentId() != null) {
                Comment parent = commentRepository.findOne(comment.getParentId());
                if (parent != null) {
                    sendNotice(parent.getAuthorId(), Notice.Type.REPLY, comment.getId());
                }
            }
        } catch (Exception e) {
            log.error(String.format("Unable to notify author of parent of %s. Reason: %s", comment, e.getMessage()));
        }
    }

    @Async
    public void notifyAsync(String recipientId, Notice.Type type, String commentId) {
        try {
            Asserts.isNotNull(recipientId, "recipientId");
            Asserts.isNotNull(type, "type");
            Asserts.isNotNull(commentId, "commentId");

            sendNotice(recipientId, type, commentId);

        } catch (Exception e) {
            log.error(String.format("Unable to notify %s with %s of %s. Reason: %s", recipientId, type, commentId, e.getMessage()));
        }
    }

    @Async
    public void notifySuperModsOfFraudulentComment(Comment comment) {
        try {
            Asserts.isNotNull(comment, "comment");
            userRepository.findSuperMods().forEach(user -> sendNotice(user.getLogin(), Notice.Type.REPORT, comment.getId()));
        } catch (Exception e) {
            log.error(String.format("Unable to notify superMods of fraud-comment %s. Reason: %s", comment, e.getMessage()));
        }
    }

    @Async
    public void notifySuperModsOfFraudulentUser(User user) {
        try {

            Asserts.isNotNull(user, "user");
            userRepository.findSuperMods().forEach(mod -> sendNotice(mod.getLogin(), Notice.Type.FRAUDULENT_USER, user.getLogin()));

        } catch (Exception e) {
            log.error(String.format("Unable to notify superMods of fraud-user %s. Reason: %s", user, e.getMessage()));
        }
    }

    @Async
    public void notifyModsOfThread(Thread thread, Comment comment) {
        try {
            Asserts.isNotNull(thread, "thread");
            Asserts.isNotNull(thread.getModIds(), "modIds");
            Asserts.isNotNull(comment, "comment");

            thread.getModIds().forEach(modId -> sendNotice(modId, Notice.Type.REVIEW, comment.getId()));

        } catch (Exception e) {
            log.error(String.format("Unable to notify mods (thread %s) of comment %s. Reason: %s", thread, comment, e.getMessage()));
        }
    }

    @Async
    public void setAllSeen(String userId) {
        try {

            Asserts.isCurrentLogin(userId);

            userRepositoryCustom.setAllNoticesSeen(userId);

        } catch (Exception e) {
            log.error(String.format("Unable to set notices of user %s as seen. Reason: %s", userId, e.getMessage()));
        }
    }

    public Boolean hasUnseen(String login) {
        return userRepository.countUnseenOfUser(login) > 0;
    }

    // --

    private void sendNotice(String recipientId, Notice.Type type, String resourceId) {

        // todo should be debug
        log.info(String.format("Notify %s of %s on resource %s", recipientId, type.name(), resourceId));

        Notice notice = new Notice();
        notice.setRecipientId(recipientId);
        notice.setInitiatorId(SecurityUtils.getCurrentLogin());
        notice.setResourceId(resourceId);
        notice.setType(type);

        noticeRepository.save(notice);
    }
}
