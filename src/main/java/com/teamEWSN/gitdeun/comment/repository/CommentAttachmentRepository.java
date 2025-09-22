package com.teamEWSN.gitdeun.comment.repository;

import com.teamEWSN.gitdeun.comment.entity.CommentAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentAttachmentRepository extends JpaRepository<CommentAttachment, Long> {
}
