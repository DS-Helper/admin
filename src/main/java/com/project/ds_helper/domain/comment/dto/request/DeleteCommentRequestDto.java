package com.project.ds_helper.domain.comment.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class DeleteCommentRequestDto {

    @Schema(
            description = "댓글 ID",
            example = "3f9c7c8e-4d5a-4b1e-9a2f-7c1d2e8f9a01"
    )
    @NotBlank
    private String commentId;
}
