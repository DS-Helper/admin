package com.project.ds_helper.common.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

@Schema(description = "응답 코드 정보 DTO")
public record ResponseCodeInfoDto(
        @Schema(description = "응답 코드", example = "OK")
        String code,

        @Schema(description = "응답 메시지", example = "요청이 성공했습니다.")
        String message,

        @Schema(description = "HTTP 상태", example = "OK")
        HttpStatus httpStatus
) {
    public static ResponseCodeInfoDto from(ResponseCode responseCode) {
        return new ResponseCodeInfoDto(
                responseCode.getCode(),
                responseCode.getMessage(),
                responseCode.getHttpStatus()
        );
    }
}
