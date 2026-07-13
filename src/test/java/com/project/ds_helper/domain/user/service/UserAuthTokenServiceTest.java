package com.project.ds_helper.domain.user.service;

import com.project.ds_helper.common.enums.JwtTokenType;
import com.project.ds_helper.common.util.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAuthTokenServiceTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private UserAuthTokenService userAuthTokenService;

    @Test
    @DisplayName("JWT 생성 후 redis와 헤더에 토큰을 저장한다")
    void generateJwtTokenAndPutInResponseHeader_setsRedisAndHeaders() {
        when(jwtUtil.generateAccessToken("user-1", "USER", "PERSONAL")).thenReturn("access");
        when(jwtUtil.generateRefreshToken("user-1", "USER", "PERSONAL")).thenReturn("refresh");
        when(jwtUtil.toRedisRefreshTokenKey("user-1")).thenReturn("refresh:user-1");
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        userAuthTokenService.generateJwtTokenAndPutInResponseHeader(response, "user-1", "USER", "PERSONAL");

        verify(valueOperations).set("refresh:user-1", "refresh");
        verify(response).setHeader(JwtTokenType.ACCESS_TOKEN_NAME.getTokenName(), "access");
        verify(response).setHeader(JwtTokenType.REFRESH_TOKEN_NAME.getTokenName(), "refresh");
    }

    @Test
    @DisplayName("로그인 체크는 redis refresh token이 유효하면 true를 반환한다")
    void checkIfUserLoggedIn_returnsTrueWhenRedisTokenValid() {
        when(jwtUtil.isExpired("refresh-token")).thenReturn(false);
        when(jwtUtil.getId("refresh-token")).thenReturn("user-1");
        when(jwtUtil.toRedisRefreshTokenKey("user-1")).thenReturn("refresh:user-1");
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("refresh:user-1")).thenReturn("refresh-token");

        Object result = userAuthTokenService.checkIfUserLoggedIn("refresh-token");

        assertThat(result).isEqualTo(true);
    }

    @Test
    @DisplayName("로그인 체크는 refresh token이 비어 있으면 false를 반환한다")
    void checkIfUserLoggedIn_returnsFalseWhenRefreshTokenBlank() {
        assertThat(userAuthTokenService.checkIfUserLoggedIn("")).isEqualTo(false);
    }

    @Test
    @DisplayName("로그인 체크는 만료된 refresh token이면 false를 반환한다")
    void checkIfUserLoggedIn_returnsFalseWhenTokenExpired() {
        when(jwtUtil.isExpired("refresh-token")).thenReturn(true);

        assertThat(userAuthTokenService.checkIfUserLoggedIn("refresh-token")).isEqualTo(false);
    }

    @Test
    @DisplayName("로그인 체크는 redis 토큰이 없으면 false를 반환한다")
    void checkIfUserLoggedIn_returnsFalseWhenRedisTokenMissing() {
        when(jwtUtil.isExpired("refresh-token")).thenReturn(false);
        when(jwtUtil.getId("refresh-token")).thenReturn("user-1");
        when(jwtUtil.toRedisRefreshTokenKey("user-1")).thenReturn("refresh:user-1");
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("refresh:user-1")).thenReturn(null);

        assertThat(userAuthTokenService.checkIfUserLoggedIn("refresh-token")).isEqualTo(false);
    }
}
