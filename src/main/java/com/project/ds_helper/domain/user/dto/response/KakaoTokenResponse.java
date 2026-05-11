package com.project.ds_helper.domain.user.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

// 토큰 응답
@Data
@Schema(description = "카카오 토큰 응답 DTO", example = "{\"access_token\":\"kakao-access-token\",\"refresh_token\":\"kakao-refresh-token\",\"expires_in\":21599,\"token_type\":\"bearer\",\"id_token\":\"id-token-value\",\"scope\":\"account_email profile_nickname\",\"refresh_token_expires_in\":\"5183999\"}")
public class KakaoTokenResponse {
    @JsonProperty("access_token")
    private String accessToken;
    @JsonProperty("refresh_token")
    private String refreshToken;
    @JsonProperty("expires_in")
    private int expiresIn;
    @JsonProperty("token_type")
    private String tokenType;
    @JsonProperty("id_token")
    private String idToken;
    @JsonProperty("scope")
    private String scope;
    @JsonProperty("refresh_token_expires_in")
    private String refreshTokenExpiresIn;

    // … 기타 필드
}
