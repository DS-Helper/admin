package com.project.ds_helper.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "카카오 로그인 요청 DTO", example = "{\"code\":\"kakao-authorization-code\"}")
public record KakaoCodeLoginRequestDto(
        @Schema(description = "카카오 authorization code", example = "kakao-authorization-code")
        String code
) {
}
