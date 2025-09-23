package com.teamEWSN.gitdeun.comment.dto;

import com.teamEWSN.gitdeun.comment.entity.AttachmentType;
import lombok.*;

public class CommentAttachmentResponseDtos {

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    public static class AttachmentResponse {
        private Long attachmentId;
        private String url;
        private String fileName;
        private AttachmentType attachmentType;
    }
}
