package com.teamEWSN.gitdeun.comment.repository;

import com.teamEWSN.gitdeun.comment.entity.Comment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = {"user", "replies"})
    List<Comment> findByCodeReviewId(Long reviewId);
}