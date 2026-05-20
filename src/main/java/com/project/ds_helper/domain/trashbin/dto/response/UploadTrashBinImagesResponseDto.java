package com.project.ds_helper.domain.trashbin.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "쓰레기통 이미지 복수 업로드 응답 DTO")
public record UploadTrashBinImagesResponseDto(
        @Schema(description = "요청 이미지 개수", example = "3")
        int requestedCount,

        @Schema(description = "신규 업로드 성공 개수", example = "2")
        int uploadedCount,

        @Schema(description = "이미 저장되어 있어 중복 처리된 개수", example = "1")
        int alreadyExistsCount,

        @Schema(description = "이미지별 업로드 처리 결과")
        List<UploadTrashBinImageResponseDto> images
) {
    public static UploadTrashBinImagesResponseDto from(List<UploadTrashBinImageResponseDto> images) {
        int uploadedCount = (int) images.stream()
                .filter(image -> !image.alreadyExists())
                .count();
        int alreadyExistsCount = images.size() - uploadedCount;

        return new UploadTrashBinImagesResponseDto(
                images.size(),
                uploadedCount,
                alreadyExistsCount,
                images
        );
    }
}
