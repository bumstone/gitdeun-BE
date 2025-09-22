package com.teamEWSN.gitdeun.comment.dto;

import com.teamEWSN.gitdeun.comment.entity.EmojiType;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class CommentRequestDtos {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotEmpty(message = "댓글 내용은 비워둘 수 없습니다.")
        private String content;
        private Long parentId;
        private EmojiType emojiType; // 최상위 댓글(리뷰의 첫 댓글)에만 사용
    }

    @Getter
    @NoArgsConstructor
    public static class UpdateRequest {
        @NotEmpty(message = "댓글 내용은 비워둘 수 없습니다.")
        private String content;
    }

    @Getter
    @NoArgsConstructor
    public static class EmojiRequest {
        private EmojiType emojiType;
    }
}
