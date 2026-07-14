package com.project.ds_helper.domain.volunteer.file.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관리자가 업로드한 공개 봉사 일정 이미지")
public record VolunteerEventImageUploadResponse(
        @Schema(description = "봉사 파일 ID", example = "8a3f1b2a-2c7d-4d6d-9d1a-3a2c7c9f1111") String volunteerFileId,
        @Schema(description = "S3 객체 키", example = "images/volunteer-event-8a3f1b2a.webp") String s3Key,
        @Schema(description = "만료되는 S3 presigned GET URL", example = "https://bucket.s3.ap-northeast-2.amazonaws.com/images/volunteer-event-8a3f1b2a.webp?X-Amz-Signature=...") String url,
        @Schema(description = "저장 MIME 타입", example = "image/webp") String contentType,
        @Schema(description = "저장 이미지 너비", example = "1920") int width,
        @Schema(description = "저장 이미지 높이", example = "1080") int height
) {
}
