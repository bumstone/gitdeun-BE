package com.teamEWSN.gitdeun.comment.service;

import com.teamEWSN.gitdeun.codereview.entity.CodeReview;
import com.teamEWSN.gitdeun.codereview.repository.CodeReviewRepository;
import com.teamEWSN.gitdeun.comment.dto.CommentRequestDtos.*;
import com.teamEWSN.gitdeun.comment.dto.CommentResponseDtos.*;
import com.teamEWSN.gitdeun.comment.dto.CommentAttachmentResponseDtos.*;
import com.teamEWSN.gitdeun.comment.entity.Comment;
import com.teamEWSN.gitdeun.comment.entity.CommentAttachment;
import com.teamEWSN.gitdeun.comment.entity.EmojiType;
import com.teamEWSN.gitdeun.comment.mapper.CommentMapper;
import com.teamEWSN.gitdeun.comment.repository.CommentAttachmentRepository;
import com.teamEWSN.gitdeun.comment.repository.CommentRepository;
import com.teamEWSN.gitdeun.common.exception.ErrorCode;
import com.teamEWSN.gitdeun.common.exception.GlobalException;
import com.teamEWSN.gitdeun.mindmapmember.service.MindmapAuthService;
import com.teamEWSN.gitdeun.user.entity.User;
import com.teamEWSN.gitdeun.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;
    private final CommentAttachmentRepository commentAttachmentRepository;
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
        // 부모 댓글이 있는 경우
        if (request.getParentId() != null) {
            parent = commentRepository.findById(request.getParentId())
                .orElseThrow(() -> new GlobalException(ErrorCode.COMMENT_NOT_FOUND));
            // 이모지 여부
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

        // 파일 첨부 로직 : 함께 첨부된 파일이 있으면, 파일을 S3에 업로드하고 CommentAttachment로 저장
        attachmentService.saveAttachments(savedComment, files);

        return commentMapper.toResponseDto(savedComment);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getCommentsAsTree(Long reviewId) {
        // 1. 리뷰에 속한 모든 댓글을 한 번에 조회합니다.
        List<Comment> allComments = commentRepository.findByCodeReviewId(reviewId);

        // 2. 댓글을 DTO로 변환하고, 댓글 ID를 키로 하는 맵을 생성합니다.
        Map<Long, CommentResponse> commentMap = allComments.stream()
            .map(commentMapper::toResponseDto)
            .collect(Collectors.toMap(CommentResponse::getCommentId, c -> c));

        // 3. 최상위 댓글과 대댓글을 분리하고, 대댓글을 부모의 replies 리스트에 추가합니다.
        List<CommentResponse> topLevelComments = new ArrayList<>();
        commentMap.values().forEach(commentDto -> {
            Long parentId = commentDto.getParentId();
            if (parentId != null) {
                CommentResponse parentDto = commentMap.get(parentId);
                if (parentDto != null) {
                    // 부모의 replies 리스트가 null이면 초기화
                    if (parentDto.getReplies() == null) {
                        parentDto.setReplies(new ArrayList<>());
                    }
                    parentDto.getReplies().add(commentDto);
                }
            } else {
                topLevelComments.add(commentDto);
            }
        });

        return topLevelComments;
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

        // 1. 댓글에 첨부된 파일이 있으면 S3와 DB에서 모두 삭제
        if (!CollectionUtils.isEmpty(comment.getAttachments())) {
            attachmentService.deleteAttachments(comment.getAttachments());
        }

        // 2. 댓글을 Soft Delete 처리
        commentRepository.delete(comment);
    }

    /**
     * 첨부파일 개별 삭제
     */
    @Transactional
    public void deleteAttachment(Long attachmentId, Long userId) {
        CommentAttachment attachment = commentAttachmentRepository.findById(attachmentId)
            .orElseThrow(() -> new GlobalException(ErrorCode.FILE_NOT_FOUND));

        boolean isAuthor = attachment.getComment().getUser().getId().equals(userId);
        boolean isManager = mindmapAuthService.hasEdit(attachment.getComment().getCodeReview().getMindmap().getId(), userId);

        if (!isAuthor && !isManager) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        attachmentService.deleteAttachments(List.of(attachment));
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