package com.project.ds_helper.domain.user.dto.request;

import com.project.ds_helper.domain.user.enums.OauthType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "모바일 구글 로그인 요청 DTO", example = "{\"provider\":\"GOOGLE\",\"accessToken\":\"google-access-token-value\"}")
public record MobileGoogleLoginRequestDto(
        OauthType provider,
        String accessToken,
        String refreshToken
) {
}
