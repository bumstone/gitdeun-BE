package com.teamEWSN.gitdeun.comment.controller;

import com.teamEWSN.gitdeun.comment.dto.CommentRequestDtos.*;
import com.teamEWSN.gitdeun.comment.dto.CommentResponseDtos.*;
import com.teamEWSN.gitdeun.comment.dto.CommentAttachmentResponseDtos.*;
import com.teamEWSN.gitdeun.comment.service.CommentService;
import com.teamEWSN.gitdeun.common.jwt.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // 특정 코드 리뷰에 댓글 또는 대댓글 작성
    @PostMapping(value = "/code-reviews/{reviewId}/comments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CommentResponse> createComment(
        @PathVariable Long reviewId,
        @Valid @RequestPart("request") CreateRequest request,
        @RequestPart(value = "files", required = false) List<MultipartFile> files,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.createComment(reviewId, userDetails.getId(), request, files));
    }

    // 댓글 내용 수정
    @PatchMapping("/comments/{commentId}")
    public ResponseEntity<CommentResponse> updateComment(
        @PathVariable Long commentId,
        @Valid @RequestBody UpdateRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(commentService.updateComment(commentId, userDetails.getId(), request));
    }

    // 댓글 삭제
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
        @PathVariable Long commentId,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        commentService.deleteComment(commentId, userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    // 댓글 스레드 해결 상태 변경 (토글)
    @PatchMapping("/comments/{commentId}/resolve")
    public ResponseEntity<Void> toggleResolveComment(
        @PathVariable Long commentId,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        commentService.toggleResolveThread(commentId, userDetails.getId());
        return ResponseEntity.ok().build();
    }

    // 특정 댓글에 파일 첨부
    @PostMapping("/comments/{commentId}/attachments")
    public ResponseEntity<List<AttachmentResponse>> uploadAttachments(
        @PathVariable Long commentId,
        @RequestParam("files") List<MultipartFile> files,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.addAttachmentsToComment(commentId, userDetails.getId(), files));
    }

    // 첨부파일 개별 삭제
    @DeleteMapping("/comments/attachments/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(
        @PathVariable Long attachmentId,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        commentService.deleteAttachment(attachmentId, userDetails.getId());
        return ResponseEntity.noContent().build();
    }

    // 최상위 댓글에 이모지 추가/변경/삭제
    @PatchMapping("/comments/{commentId}/emoji")
    public ResponseEntity<Void> updateEmoji(
        @PathVariable Long commentId,
        @Valid @RequestBody EmojiRequest request,
        @AuthenticationPrincipal CustomUserDetails userDetails) {
        commentService.updateEmoji(commentId, userDetails.getId(), request.getEmojiType());
        return ResponseEntity.ok().build();
    }
}
