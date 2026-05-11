package com.project.ds_helper.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "네이버 로그인 요청 DTO", example = "{\"code\":\"naver-authorization-code\",\"state\":\"naver-state-token\"}")
public record NaverCodeLoginRequestDto(
        @Schema(description = "네이버 authorization code", example = "naver-authorization-code")
        String code,
        @Schema(description = "네이버 state 값", example = "naver-state-token")
        String state
) {
}
