package com.project.ds_helper.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "구글 로그인 요청 DTO", example = "{\"code\":\"google-authorization-code\"}")
public record GoogleCodeLoginRequestDto(
        @Schema(description = "구글 authorization code", example = "google-authorization-code")
        String code
) {
}
