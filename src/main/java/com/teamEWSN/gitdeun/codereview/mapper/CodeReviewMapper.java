package com.teamEWSN.gitdeun.codereview.mapper;

import com.teamEWSN.gitdeun.codereview.dto.CodeReviewResponseDtos.*;
import com.teamEWSN.gitdeun.codereview.dto.CodeReviewRequestDtos.*;
import com.teamEWSN.gitdeun.codereview.entity.CodeReview;
import com.teamEWSN.gitdeun.comment.entity.Comment;
import com.teamEWSN.gitdeun.comment.mapper.CommentMapper;
import org.mapstruct.*;

import java.util.Comparator;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE,
    uses = {CommentMapper.class}
)
public interface CodeReviewMapper {

    @Mapping(source = "id", target = "reviewId")
    @Mapping(source = "author.nickname", target = "authorNickname")
    @Mapping(source = "author.profileImage", target = "authorProfileImage")
    @Mapping(source = "codeReference.id", target = "codeReferenceId")
    ReviewResponse toReviewResponse(CodeReview codeReview);

    @Mapping(source = "id", target = "reviewId")
    @Mapping(source = "author.nickname", target = "authorNickname")
    ReviewListResponse toReviewListResponse(CodeReview codeReview);

    @AfterMapping
    default void afterMapping(@MappingTarget ReviewListResponse.ReviewListResponseBuilder builder, CodeReview review) {
        // 첫 번째 댓글(최상위 댓글)을 찾아 내용 설정
        review.getComments().stream()
            .filter(Comment::isTopLevel)
            .min(Comparator.comparing(Comment::getCreatedAt))
            .ifPresent(comment -> builder.firstCommentContent(comment.getContent()));

        // 전체 댓글 수 설정
        builder.commentCount(review.getComments().size());
    }
}

