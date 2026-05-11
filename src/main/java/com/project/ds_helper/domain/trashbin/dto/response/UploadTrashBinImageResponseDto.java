package com.project.ds_helper.domain.trashbin.dto.response;

import com.project.ds_helper.domain.trashbin.entity.TrashBin;
import com.project.ds_helper.domain.trashbin.entity.TrashBinImage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "쓰레기통 이미지 업로드 응답 DTO")
public record UploadTrashBinImageResponseDto(
        @Schema(description = "이미지가 매핑되었거나 이미 매핑되어 있던 쓰레기통 ID", example = "3f9c7c8e-4d5a-4b1e-9a2f-7c1d2e8f9a01")
        String trashBinId,

        @Schema(description = "파일명에서 파싱한 매핑 기준 위도", example = "35.808057")
        Double latitude,

        @Schema(description = "매핑된 쓰레기통 경도", example = "128.508827")
        Double longitude,

        @Schema(description = "동일 위도 이미지가 이미 저장되어 있었는지 여부", example = "false")
        boolean alreadyExists,

        @Schema(description = "업로드/중복 처리 결과 메시지", example = "Image uploaded successfully")
        String message,

        @Schema(description = "이미지 ID", example = "8dece4df-d2fb-4a4d-a908-8aa7b8e7b922")
        String imageId,

        @Schema(description = "원본 파일명", example = "35.808057.png")
        String originalName,

        @Schema(description = "S3 저장 파일명", example = "20260503120000_uuid")
        String storedName,

        @Schema(description = "S3 object key", example = "images/20260503120000_uuid")
        String s3Key,

        @Schema(description = "S3 이미지 URL", example = "https://bucket.s3.ap-northeast-2.amazonaws.com/images/20260503120000_uuid")
        String imageUrl,

        @Schema(description = "압축 후 파일 크기", example = "4521")
        Long size,

        @Schema(description = "압축 후 content type", example = "image/webp")
        String contentType
) {

    public static UploadTrashBinImageResponseDto uploaded(TrashBin trashBin, TrashBinImage image) {
        return toDto(trashBin, image, false, "Image uploaded successfully");
    }

    public static UploadTrashBinImageResponseDto alreadyExists(TrashBinImage image) {
        return toDto(image.getTrashBin(), image, true, "Image already exists for latitude");
    }

    private static UploadTrashBinImageResponseDto toDto(
            TrashBin trashBin,
            TrashBinImage image,
            boolean alreadyExists,
            String message
    ) {
        return UploadTrashBinImageResponseDto.builder()
                .trashBinId(trashBin.getId())
                .latitude(trashBin.getLatitude())
                .longitude(trashBin.getLongitude())
                .alreadyExists(alreadyExists)
                .message(message)
                .imageId(image.getId())
                .originalName(image.getOriginalName())
                .storedName(image.getStoredName())
                .s3Key(image.getS3Key())
                .imageUrl(image.getUrl())
                .size(image.getSize())
                .contentType(image.getContentType())
                .build();
    }
}
