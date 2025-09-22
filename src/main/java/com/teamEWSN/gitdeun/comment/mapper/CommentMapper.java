package com.teamEWSN.gitdeun.comment.mapper;

import com.teamEWSN.gitdeun.comment.dto.CommentResponseDtos.CommentResponse;
import com.teamEWSN.gitdeun.comment.dto.CommentAttachmentResponseDtos.AttachmentResponse;
import com.teamEWSN.gitdeun.comment.entity.Comment;
import com.teamEWSN.gitdeun.comment.entity.CommentAttachment;
import org.mapstruct.*;

import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CommentMapper {

    @Mapping(source = "id", target = "commentId")
    @Mapping(source = "parentComment.id", target = "parentId")
    @Mapping(source = "user.nickname", target = "authorNickname")
    @Mapping(source = "user.profileImage", target = "authorProfileImage")
    CommentResponse toResponseDto(Comment comment);

    @Mapping(source = "id", target = "attachmentId")
    AttachmentResponse toAttachmentResponseDto(CommentAttachment attachment);

    List<AttachmentResponse> toAttachmentResponseDtoList(List<CommentAttachment> attachments);

    @AfterMapping
    default void afterMapping(Comment comment, @MappingTarget CommentResponse.CommentResponseBuilder builder) {
        // 삭제된 댓글일 경우, 내용을 고정된 메시지로 변경
        if (comment.getDeletedAt() != null) {
            builder.content("삭제된 댓글입니다.");
            builder.authorNickname("알 수 없음"); // 작성자 정보도 가림
            builder.authorProfileImage(null);
            builder.attachments(null); // 첨부파일도 숨김
        }

        // 대댓글 목록이 있는 경우, 재귀적으로 DTO로 변환하여 설정
        if (comment.getReplies() != null && !comment.getReplies().isEmpty()) {
            builder.replies(comment.getReplies().stream()
                .map(this::toResponseDto) // 각 대댓글에 대해 toResponseDto를 재귀적으로 호출
                .collect(Collectors.toList()));
        }
    }
}