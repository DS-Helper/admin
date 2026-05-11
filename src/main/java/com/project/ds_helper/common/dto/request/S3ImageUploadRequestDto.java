package com.project.ds_helper.common.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@Schema(
        description = "S3 이미지 업로드 요청 DTO",
        example = "{\"storedFilename\":\"20260323120000_uuid\",\"originalFilename\":\"image.png\",\"s3ContentType\":\"image/png\",\"fileExtension\":\"png\",\"size\":1024,\"bytes\":\"<binary>\"}"
)
public class S3ImageUploadRequestDto {

    /**
     * 압축 이후의 실제 업로드 정보만 유지한다.
     * 서비스 레이어는 이 DTO 하나로 S3 업로드와 DB 메타데이터 저장을 함께 맞춘다.
     */
    @NotBlank
    private String storedFilename;

    @NotBlank
    private String originalFilename;

    @NotNull
    private byte[] bytes;

    @NotNull
    private Long size;

    @NotBlank
    private String s3ContentType;

    @NotBlank
    private String fileExtension;
}
