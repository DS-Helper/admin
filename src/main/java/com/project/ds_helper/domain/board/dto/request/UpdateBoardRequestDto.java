package com.project.ds_helper.domain.board.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Setter
public class UpdateBoardRequestDto {


    @Schema(
            description = "게시글 ID",
            example = "3f9c7c8e-4d5a-4b1e-9a2f-7c1d2e8f9a01"
    )
    @NotBlank
    private String boardId;

    @Schema(
            description = "제목",
            example = "제목입니다."
    )
    @NotBlank
    private String title;

    @Schema(
            description = "내용",
            example = "내용입니다."
    )
    @NotBlank
    private String content;

    @Schema(
            description = "유지할 기존 S3 이미지 URL 리스트",
            example = """
                    "https://ds-helper-bucket.s3.ap-northeast-2.amazonaws.com/images/2026/02/21/uuid1.jpg",
                            "https://ds-helper-bucket.s3.ap-northeast-2.amazonaws.com/images/2026/02/21/uuid2.jpg",
                            "https://ds-helper-bucket.s3.ap-northeast-2.amazonaws.com/images/2026/02/21/uuid3.jpg"
                    """
    )
    // 유지할 기존 이미지 S3 URL 목록
    private List<String> keepImageUrls;
}
