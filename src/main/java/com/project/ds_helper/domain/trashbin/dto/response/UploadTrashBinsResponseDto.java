package com.project.ds_helper.domain.trashbin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "쓰레기통 CSV 업로드 응답 DTO")
public record UploadTrashBinsResponseDto(
        @Schema(description = "저장된 쓰레기통 개수", example = "120")
        int savedCount
) {
}
