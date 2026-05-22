package com.project.ds_helper.domain.user.dto.request;

import com.project.ds_helper.domain.user.enums.OauthType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "모바일 네이버 로그인 요청 DTO", example = "{\"provider\":\"NAVER\",\"accessToken\":\"naver-access-token-value\"}")
public record MobileNaverLoginRequestDto(
        OauthType provider,
        String accessToken,
        String refreshToken
) {
}
