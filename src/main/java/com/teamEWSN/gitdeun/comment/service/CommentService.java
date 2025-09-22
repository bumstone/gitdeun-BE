package com.teamEWSN.gitdeun.comment.service;

import com.teamEWSN.gitdeun.codereview.entity.CodeReview;
import com.teamEWSN.gitdeun.codereview.repository.CodeReviewRepository;
import com.teamEWSN.gitdeun.comment.dto.CommentRequestDtos.*;
import com.teamEWSN.gitdeun.comment.dto.CommentResponseDtos.*;
import com.teamEWSN.gitdeun.comment.dto.CommentAttachmentResponseDtos.*;
import com.teamEWSN.gitdeun.comment.entity.Comment;
import com.teamEWSN.gitdeun.comment.entity.EmojiType;
import com.teamEWSN.gitdeun.comment.mapper.CommentMapper;
import com.teamEWSN.gitdeun.comment.repository.CommentRepository;
import com.teamEWSN.gitdeun.common.exception.ErrorCode;
import com.teamEWSN.gitdeun.common.exception.GlobalException;
import com.teamEWSN.gitdeun.mindmapmember.service.MindmapAuthService;
import com.teamEWSN.gitdeun.user.entity.User;
import com.teamEWSN.gitdeun.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final CodeReviewRepository codeReviewRepository;
    private final UserRepository userRepository;
    private final MindmapAuthService mindmapAuthService;
    private final CommentAttachmentService attachmentService;
    private final CommentMapper commentMapper;

    @Transactional
    public CommentResponse createComment(Long reviewId, Long userId, CreateRequest request, List<MultipartFile> files) {
        CodeReview review = codeReviewRepository.findById(reviewId)
            .orElseThrow(() -> new GlobalException(ErrorCode.CODE_REVIEW_NOT_FOUND));

        if (!mindmapAuthService.hasEdit(review.getMindmap().getId(), userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        User author = userRepository.findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND_BY_ID));

        Comment parent = null;
        if (request.getParentId() != null) {
            parent = commentRepository.findById(request.getParentId())
                .orElseThrow(() -> new GlobalException(ErrorCode.COMMENT_NOT_FOUND));
            if (request.getEmojiType() != null) {
                throw new GlobalException(ErrorCode.EMOJI_ON_REPLY_NOT_ALLOWED);
            }
        }

        Comment comment = Comment.builder()
            .codeReview(review)
            .user(author)
            .parentComment(parent)
            .content(request.getContent())
            .emojiType(request.getEmojiType())
            .build();

        Comment savedComment = commentRepository.save(comment);

        // 파일 첨부 로직
        attachmentService.saveAttachments(savedComment, files);

        return commentMapper.toResponseDto(savedComment);
    }

    @Transactional(readOnly = true)
    public List<AttachmentResponse> addAttachmentsToComment(Long commentId, Long userId, List<MultipartFile> files) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new GlobalException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getUser().getId().equals(userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        return commentMapper.toAttachmentResponseDtoList(attachmentService.saveAttachments(comment, files));
    }


    @Transactional
    public CommentResponse updateComment(Long commentId, Long userId, UpdateRequest request) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new GlobalException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getUser().getId().equals(userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        comment.updateContent(request.getContent());
        return commentMapper.toResponseDto(comment);
    }

    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new GlobalException(ErrorCode.COMMENT_NOT_FOUND));

        boolean isAuthor = comment.getUser().getId().equals(userId);
        boolean isManager = mindmapAuthService.hasEdit(comment.getCodeReview().getMindmap().getId(), userId);

        if (!isAuthor && !isManager) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        commentRepository.delete(comment);
    }

    @Transactional
    public void toggleResolveThread(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new GlobalException(ErrorCode.COMMENT_NOT_FOUND));

        if (comment.isReply()) {
            throw new GlobalException(ErrorCode.CANNOT_RESOLVE_REPLY);
        }

        if (!mindmapAuthService.hasEdit(comment.getCodeReview().getMindmap().getId(), userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        comment.toggleResolve();
    }

    @Transactional
    public void updateEmoji(Long commentId, Long userId, EmojiType emojiType) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new GlobalException(ErrorCode.COMMENT_NOT_FOUND));

        if (comment.isReply()) {
            throw new GlobalException(ErrorCode.EMOJI_ON_REPLY_NOT_ALLOWED);
        }

        boolean isAuthor = comment.getUser().getId().equals(userId);
        boolean isReviewAuthor = comment.getCodeReview().getAuthor().getId().equals(userId);

        if (!isAuthor && !isReviewAuthor) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        comment.updateEmoji(emojiType);
    }
}