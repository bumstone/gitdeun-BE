package com.teamEWSN.gitdeun.codereview.dto;

import com.teamEWSN.gitdeun.codereview.entity.CodeReviewStatus;
import com.teamEWSN.gitdeun.comment.entity.EmojiType;
import jakarta.validation.constraints.NotEmpty;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class CodeReviewRequestDtos {
    @Getter
    @NoArgsConstructor
    public static class CreateRequest {
        @NotEmpty(message = "댓글 내용은 비워둘 수 없습니다.")
        private String content;
        private EmojiType emojiType;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    public static class StatusRequest {
        private CodeReviewStatus status;
    }
}