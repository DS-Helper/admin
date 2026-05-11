package com.project.ds_helper.domain.user.controller;

import com.project.ds_helper.common.dto.response.ResponseVo;
import com.project.ds_helper.common.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthTokenControllerTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @InjectMocks
    private AuthTokenController authTokenController;

    @Test
    @DisplayName("refresh token이 없으면 unauthorized 응답을 반환한다")
    void reGenerateJWT_returnsUnauthorizedWhenTokenMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtUtil.getRefreshTokenFromRequestHeader(request)).thenReturn(null);

        ResponseEntity<ResponseVo> result = authTokenController.reGenerateJWT(request, response);

        assertThat(result.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    @DisplayName("refresh token 타입이 아니면 unauthorized 응답을 반환한다")
    void reGenerateJWT_returnsUnauthorizedWhenTokenTypeInvalid() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtUtil.getRefreshTokenFromRequestHeader(request)).thenReturn("not-refresh-token");
        when(jwtUtil.isRefreshToken("not-refresh-token")).thenReturn(false);

        ResponseEntity<ResponseVo> result = authTokenController.reGenerateJWT(request, response);

        assertThat(result.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    @DisplayName("tokenInvalidResponse는 401 응답을 반환한다")
    void tokenInvalidResponse_returnsUnauthorized() {
        ResponseEntity<ResponseVo> result = authTokenController.tokenInvalidResponse();

        assertThat(result.getStatusCode().value()).isEqualTo(401);
        assertThat(result.getBody().isSuccess()).isFalse();
    }
}
