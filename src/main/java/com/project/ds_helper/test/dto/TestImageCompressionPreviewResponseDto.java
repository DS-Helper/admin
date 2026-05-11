package com.project.ds_helper.test.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "원본/압축 이미지 미리보기 응답 DTO")
public record TestImageCompressionPreviewResponseDto(
        @Schema(description = "원본 파일명", example = "sample.png")
        String originalFilename,

        @Schema(description = "원본 Content-Type", example = "image/png")
        String originalContentType,

        @Schema(description = "원본 파일 크기(byte)", example = "102400")
        long originalSize,

        @Schema(description = "원본 이미지 Data URL", example = "data:image/png;base64,...")
        String originalImageDataUrl,

        @Schema(description = "압축 후 저장 파일명", example = "20260424120000_uuid")
        String compressedStoredFilename,

        @Schema(description = "압축 후 Content-Type", example = "image/webp")
        String compressedContentType,

        @Schema(description = "압축 후 확장자", example = "webp")
        String compressedExtension,

        @Schema(description = "압축 후 파일 크기(byte)", example = "20480")
        long compressedSize,

        @Schema(description = "압축 이미지 Data URL", example = "data:image/webp;base64,...")
        String compressedImageDataUrl
) {
}
