package com.project.ds_helper.domain.user.dto.request;

import com.project.ds_helper.domain.user.enums.OauthType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "모바일 카카오 로그인 요청 DTO", example = "{\"provider\":\"KAKAO\",\"accessToken\":\"kakao-access-token-value\"}")
public record MobileKakaoLoginRequestDto(
        OauthType provider,
        String accessToken
) {

}
