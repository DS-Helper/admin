package com.project.ds_helper.domain.board.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@NoArgsConstructor
@Getter
@AllArgsConstructor
public class CreateBoardReqDto {

    @Schema(
            description = "카테고리",
            example = "맛집"
    )
    @NotEmpty
    private String category;

    @Schema(
            description = "제목",
            example = "제목입니다."
    )
    @NotEmpty
    private String title;

    @Schema(
            description = "내용",
            example = "내용입니다."
    )
    @NotEmpty
    private String content;
}
