package com.project.ds_helper.common.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "JWT 토큰 응답 DTO", example = "{\"accessToken\":\"access-token-value\",\"refreshToken\":\"refresh-token-value\"}")
public record JwtResponse(
        @Schema(description = "Access Token", example = "access-token-value")
        String accessToken,
        @Schema(description = "Refresh Token", example = "refresh-token-value")
        String refreshToken
) {
}
