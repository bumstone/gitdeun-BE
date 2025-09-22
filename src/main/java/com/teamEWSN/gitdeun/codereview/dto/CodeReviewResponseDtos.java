package com.teamEWSN.gitdeun.codereview.dto;

import com.teamEWSN.gitdeun.codereview.entity.CodeReviewStatus;
import com.teamEWSN.gitdeun.comment.dto.CommentResponseDtos.CommentResponse;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public class CodeReviewResponseDtos {

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class ReviewResponse {
        private Long reviewId;
        private String nodeId;
        private Long codeReferenceId;
        private CodeReviewStatus status;
        private String authorNickname;
        private String authorProfileImage;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private List<CommentResponse> comments;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class ReviewListResponse {
        private Long reviewId;
        private CodeReviewStatus status;
        private String authorNickname;
        private String firstCommentContent;
        private int commentCount;
        private LocalDateTime createdAt;
    }
}
