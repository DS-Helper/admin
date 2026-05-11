package com.project.ds_helper.domain.comment.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.project.ds_helper.domain.comment.entity.Comment;
import com.project.ds_helper.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CreateCommentResponseDto {

    @Schema(
            description = "댓글or대댓글 ID",
            example = "3f9c7c8e-4d5a-4b1e-9a2f-7c1d2e8f9a01"
    )
    @NotNull
    private String commentId;

    @Schema(
            description = "댓글 ID",
            example = "3f9c7c8e-4d5a-4b1e-9a2f-7c1d2e8f9a01"
    )
    @Nullable
    private String parentId;

    @Schema(
            description = "댓글 내용",
            example = "댓글 내용입니다."
    )
    @NotNull
    private String content;

    @Schema(
            description = "댓글 작성자",
            example = "김길동"
    )
    @NotNull
    private String writerName;

    @Schema(
            description = "댓글 작성 일시",
            example = "2026-01-01 16:25:38"
    )
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    public static CreateCommentResponseDto toDto(Comment comment, Comment parent, User user){
        return CreateCommentResponseDto.builder()
                .commentId(comment.getId())
                .parentId(parent != null? parent.getId() : null)
                .content(comment.getContent())
                .writerName(user.getName() != null? user.getName() : null)
                .build();
    }
}
