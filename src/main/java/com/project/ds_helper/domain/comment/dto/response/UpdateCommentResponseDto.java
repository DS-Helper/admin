package com.project.ds_helper.domain.comment.dto.response;

import com.project.ds_helper.domain.comment.entity.Comment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateCommentResponseDto {

    @Schema(
            description = "댓글 ID",
            example = "3f9c7c8e-4d5a-4b1e-9a2f-7c1d2e8f9a01"
    )
    private String commentId;

    @Schema(
            description = "댓글 내용",
            example = "댓글 내용입니다."
    )
    private String content;

    public static UpdateCommentResponseDto toDto(Comment comment){
        return UpdateCommentResponseDto.builder()
                .commentId(comment.getId())
                .content(comment.getContent())
                .build();
    }
}