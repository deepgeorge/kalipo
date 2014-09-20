package org.kalipo.service;

import org.kalipo.aop.EnableArgumentValidation;
import org.kalipo.domain.Comment;
import org.kalipo.repository.CommentRepository;
import org.kalipo.security.SecurityUtils;
import org.kalipo.web.rest.KalipoRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;

@Service
@EnableArgumentValidation
public class CommentService {

    private final Logger log = LoggerFactory.getLogger(CommentService.class);

    @Inject
    private CommentRepository commentRepository;

    public void create(Comment comment) throws KalipoRequestException {

        // todo remove id
        comment.setAuthorId(SecurityUtils.getCurrentLogin());
        comment.setStatus(Comment.Status.APPROVED);
        commentRepository.save(comment);
    }

    public void update(Comment comment) throws KalipoRequestException {

        comment.setAuthorId(SecurityUtils.getCurrentLogin());
        comment.setStatus(Comment.Status.APPROVED);
        commentRepository.save(comment);
    }

    public List<Comment> findAll() {
        return commentRepository.findAll();
    }

    public Comment get(String id) throws KalipoRequestException {
        return commentRepository.findOne(id);
    }

    public void delete(String id) {
        commentRepository.delete(id);
    }
}