package com.teamEWSN.gitdeun.codereview.service;

import com.teamEWSN.gitdeun.codereference.entity.CodeReference;
import com.teamEWSN.gitdeun.codereference.repository.CodeReferenceRepository;
import com.teamEWSN.gitdeun.codereview.dto.CodeReviewResponseDtos.*;
import com.teamEWSN.gitdeun.codereview.dto.CodeReviewRequestDtos.*;
import com.teamEWSN.gitdeun.codereview.entity.CodeReview;
import com.teamEWSN.gitdeun.codereview.entity.CodeReviewStatus;
import com.teamEWSN.gitdeun.codereview.mapper.CodeReviewMapper;
import com.teamEWSN.gitdeun.codereview.repository.CodeReviewRepository;
import com.teamEWSN.gitdeun.comment.dto.CommentRequestDtos;
import com.teamEWSN.gitdeun.comment.service.CommentService;
import com.teamEWSN.gitdeun.common.exception.ErrorCode;
import com.teamEWSN.gitdeun.common.exception.GlobalException;
import com.teamEWSN.gitdeun.mindmap.entity.Mindmap;
import com.teamEWSN.gitdeun.mindmap.repository.MindmapRepository;
import com.teamEWSN.gitdeun.mindmapmember.service.MindmapAuthService;
import com.teamEWSN.gitdeun.user.entity.User;
import com.teamEWSN.gitdeun.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CodeReviewService {
    private final CodeReviewRepository codeReviewRepository;
    private final MindmapRepository mindmapRepository;
    private final UserRepository userRepository;
    private final CodeReferenceRepository codeReferenceRepository;
    private final MindmapAuthService mindmapAuthService;
    private final CommentService commentService;
    private final CodeReviewMapper codeReviewMapper;

    @Transactional
    public ReviewResponse createNodeReview(Long mapId, String nodeKey, Long userId, CreateRequest request, List<MultipartFile> files) {
        // 리뷰 작성 권한 확인
        if (!mindmapAuthService.hasEdit(mapId, userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        Mindmap mindmap = mindmapRepository.findByIdAndDeletedAtIsNull(mapId)
            .orElseThrow(() -> new GlobalException(ErrorCode.MINDMAP_NOT_FOUND));

        User author = userRepository.findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND_BY_ID));

        CodeReview codeReview = CodeReview.builder()
            .mindmap(mindmap)
            .author(author)
            .nodeKey(nodeKey)
            .status(CodeReviewStatus.PENDING)
            .build();
        CodeReview savedReview = codeReviewRepository.save(codeReview);

        // CommentService를 통해 첫 댓글 생성
        CommentRequestDtos.CreateRequest commentRequest = new CommentRequestDtos.CreateRequest(request.getContent(), null, request.getEmojiType());
        commentService.createComment(savedReview.getId(), userId, commentRequest, files);

        return getReview(savedReview.getId(), userId);
    }

    @Transactional
    public ReviewResponse createReferenceReview(Long refId, Long userId, CreateRequest request, List<MultipartFile> files) {
        CodeReference codeReference = codeReferenceRepository.findById(refId)
            .orElseThrow(() -> new GlobalException(ErrorCode.CODE_REFERENCE_NOT_FOUND));

        Long mapId = codeReference.getMindmap().getId();
        // 리뷰 작성 권한 확인
        if (!mindmapAuthService.hasEdit(mapId, userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        Mindmap mindmap = mindmapRepository.findByIdAndDeletedAtIsNull(mapId)
            .orElseThrow(() -> new GlobalException(ErrorCode.MINDMAP_NOT_FOUND));

        User author = userRepository.findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new GlobalException(ErrorCode.USER_NOT_FOUND_BY_ID));

        CodeReview codeReview = CodeReview.builder()
            .mindmap(mindmap)
            .author(author)
            .codeReference(codeReference)
            .status(CodeReviewStatus.PENDING)
            .build();
        CodeReview savedReview = codeReviewRepository.save(codeReview);

        // CommentService를 통해 첫 댓글과 첨부파일을 함께 생성
        CommentRequestDtos.CreateRequest commentRequest = new CommentRequestDtos.CreateRequest(request.getContent(), null, request.getEmojiType());
        commentService.createComment(savedReview.getId(), userId, commentRequest, files);

        return getReview(savedReview.getId(), userId);
    }

    @Transactional(readOnly = true)
    public ReviewResponse getReview(Long reviewId, Long userId) {
        CodeReview review = codeReviewRepository.findById(reviewId)
            .orElseThrow(() -> new GlobalException(ErrorCode.CODE_REVIEW_NOT_FOUND));

        if (!mindmapAuthService.hasView(review.getMindmap().getId(), userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        ReviewResponse response = codeReviewMapper.toReviewResponse(review);
        // CommentService를 통해 계층 구조의 댓글 목록을 가져와 설정
        response.setComments(commentService.getCommentsAsTree(review.getId()));
        return response;
    }

    @Transactional
    public void updateReviewStatus(Long reviewId, Long userId, CodeReviewStatus status) {
        CodeReview review = codeReviewRepository.findById(reviewId)
            .orElseThrow(() -> new GlobalException(ErrorCode.CODE_REVIEW_NOT_FOUND));

        if (!mindmapAuthService.hasEdit(review.getMindmap().getId(), userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        review.changeStatus(status);
    }

    @Transactional(readOnly = true)
    public Page<ReviewListResponse> getReviewsForNode(Long mapId, String nodeKey, Long userId, Pageable pageable) {
        if (!mindmapAuthService.hasView(mapId, userId)) {
            throw new GlobalException(ErrorCode.FORBIDDEN_ACCESS);
        }

        Page<CodeReview> reviews = codeReviewRepository.findByMindmapIdAndNodeKey(mapId, nodeKey, pageable);
        return reviews.map(codeReviewMapper::toReviewListResponse);
    }
}
