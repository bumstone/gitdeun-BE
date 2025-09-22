package com.teamEWSN.gitdeun.comment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.teamEWSN.gitdeun.comment.entity.EmojiType;
import com.teamEWSN.gitdeun.comment.dto.CommentAttachmentResponseDtos.AttachmentResponse;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class CommentResponseDtos {

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CommentResponse {
        private Long commentId;
        private Long parentId;
        private String content;
        private String authorNickname;
        private String authorProfileImage;
        private EmojiType emojiType; // 최상위 댓글(리뷰의 첫 댓글)에만 사용
        private boolean isResolved;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private List<CommentResponse> replies;
        private List<AttachmentResponse> attachments;
    }
}