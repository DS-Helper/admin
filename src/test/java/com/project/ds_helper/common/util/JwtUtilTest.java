package com.project.ds_helper.common.util;

import com.project.ds_helper.common.enums.JwtTokenType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private final JwtUtil jwtUtil = new JwtUtil();

    @Test
    @DisplayName("access token과 refresh token은 서로 다른 값으로 발급된다")
    void generateTokens_returnsDifferentValues() {
        String accessToken = jwtUtil.generateAccessToken("user-1", "USER", "PERSONAL");
        String refreshToken = jwtUtil.generateRefreshToken("user-1", "USER", "PERSONAL");

        assertThat(accessToken).isNotEqualTo(refreshToken);
    }

    @Test
    @DisplayName("access token과 refresh token 모두 기존 파싱 메서드로 사용자 정보를 읽을 수 있다")
    void parseClaims_worksForBothAccessAndRefreshTokens() {
        String accessToken = jwtUtil.generateAccessToken("user-1", "USER", "PERSONAL");
        String refreshToken = jwtUtil.generateRefreshToken("user-1", "USER", "PERSONAL");

        assertThat(jwtUtil.getId(accessToken)).isEqualTo("user-1");
        assertThat(jwtUtil.getRole(accessToken)).isEqualTo("USER");
        assertThat(jwtUtil.getType(accessToken)).isEqualTo("PERSONAL");

        assertThat(jwtUtil.getId(refreshToken)).isEqualTo("user-1");
        assertThat(jwtUtil.getRole(refreshToken)).isEqualTo("USER");
        assertThat(jwtUtil.getType(refreshToken)).isEqualTo("PERSONAL");
        assertThat(jwtUtil.getTokenType(accessToken)).isEqualTo(JwtTokenType.ACCESS_TOKEN_NAME.getTokenName());
        assertThat(jwtUtil.getTokenType(refreshToken)).isEqualTo(JwtTokenType.REFRESH_TOKEN_NAME.getTokenName());
        assertThat(jwtUtil.isAccessToken(accessToken)).isTrue();
        assertThat(jwtUtil.isRefreshToken(accessToken)).isFalse();
        assertThat(jwtUtil.isRefreshToken(refreshToken)).isTrue();
        assertThat(jwtUtil.isAccessToken(refreshToken)).isFalse();
    }

    @Test
    @DisplayName("Authorization 헤더가 없거나 잘못되면 null을 반환한다")
    void getAccessTokenFromRequestHeader_returnsNullForMissingOrInvalidHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThat(jwtUtil.getAccessTokenFromRequestHeader(request)).isNull();

        request.addHeader("Authorization", "Basic abc");
        assertThat(jwtUtil.getAccessTokenFromRequestHeader(request)).isNull();
    }

    @Test
    @DisplayName("refresh 토큰 헤더가 없거나 비어 있으면 null을 반환한다")
    void getRefreshTokenFromRequestHeader_returnsNullForMissingOrBlankHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        assertThat(jwtUtil.getRefreshTokenFromRequestHeader(request)).isNull();

        request.addHeader(JwtTokenType.REFRESH_TOKEN_NAME.getTokenName(), "   ");
        assertThat(jwtUtil.getRefreshTokenFromRequestHeader(request)).isNull();
    }

}
