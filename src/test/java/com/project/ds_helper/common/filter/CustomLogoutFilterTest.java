package com.project.ds_helper.common.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ds_helper.common.util.CookieUtil;
import com.project.ds_helper.common.util.JwtUtil;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomLogoutFilterTest {

    @Test
    @DisplayName("로그아웃 경로가 아니면 다음 필터로 넘긴다")
    void doFilter_skipsNonLogoutPath() throws Exception {
        CustomLogoutFilter filter = new CustomLogoutFilter(
                mock(RedisTemplate.class),
                mock(JwtUtil.class),
                mock(CookieUtil.class),
                new ObjectMapper()
        );
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(new MockHttpServletRequest("GET", "/other"), new MockHttpServletResponse(), chain);

        verify(chain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("refresh token이 없으면 401 응답을 쓴다")
    void doFilter_writesUnauthorizedWhenRefreshTokenMissing() throws Exception {
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        JwtUtil jwtUtil = mock(JwtUtil.class);
        CustomLogoutFilter filter = new CustomLogoutFilter(
                redisTemplate,
                jwtUtil,
                mock(CookieUtil.class),
                new ObjectMapper()
        );
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/logout");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtil.getRefreshTokenFromRequestHeader(request)).thenReturn(null);

        filter.doFilter(request, response, mock(FilterChain.class));

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("redis에 refresh token이 없으면 401 응답을 쓴다")
    void doFilter_writesUnauthorizedWhenRedisTokenMissing() throws Exception {
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        JwtUtil jwtUtil = mock(JwtUtil.class);
        CustomLogoutFilter filter = new CustomLogoutFilter(
                redisTemplate,
                jwtUtil,
                mock(CookieUtil.class),
                new ObjectMapper()
        );
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/logout");
        request.addHeader("refreshToken", "refresh");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtil.getRefreshTokenFromRequestHeader(request)).thenReturn("refresh");
        when(jwtUtil.isExpired("refresh")).thenReturn(false);
        when(jwtUtil.isRefreshToken("refresh")).thenReturn(true);
        when(jwtUtil.getId("refresh")).thenReturn("user-1");
        when(jwtUtil.toRedisRefreshTokenKey("user-1")).thenReturn("user-1_refreshToken");
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("user-1_refreshToken")).thenReturn(null);

        filter.doFilter(request, response, mock(FilterChain.class));

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("redis 삭제 실패 시 401 응답을 쓴다")
    void doFilter_writesUnauthorizedWhenRedisDeleteFails() throws Exception {
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        JwtUtil jwtUtil = mock(JwtUtil.class);
        CustomLogoutFilter filter = new CustomLogoutFilter(
                redisTemplate,
                jwtUtil,
                mock(CookieUtil.class),
                new ObjectMapper()
        );
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/logout");
        request.addHeader("refreshToken", "refresh");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtil.getRefreshTokenFromRequestHeader(request)).thenReturn("refresh");
        when(jwtUtil.isExpired("refresh")).thenReturn(false);
        when(jwtUtil.isRefreshToken("refresh")).thenReturn(true);
        when(jwtUtil.getId("refresh")).thenReturn("user-1");
        when(jwtUtil.toRedisRefreshTokenKey("user-1")).thenReturn("user-1_refreshToken");
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("user-1_refreshToken")).thenReturn("refresh");
        when(redisTemplate.delete("user-1_refreshToken")).thenReturn(false);

        filter.doFilter(request, response, mock(FilterChain.class));

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("정상 로그아웃은 200 응답을 쓴다")
    void doFilter_writesLogoutSuccess() throws Exception {
        RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
        JwtUtil jwtUtil = mock(JwtUtil.class);
        CustomLogoutFilter filter = new CustomLogoutFilter(
                redisTemplate,
                jwtUtil,
                mock(CookieUtil.class),
                new ObjectMapper()
        );
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/logout");
        request.addHeader("refreshToken", "refresh");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtUtil.getRefreshTokenFromRequestHeader(request)).thenReturn("refresh");
        when(jwtUtil.isExpired("refresh")).thenReturn(false);
        when(jwtUtil.isRefreshToken("refresh")).thenReturn(true);
        when(jwtUtil.getId("refresh")).thenReturn("user-1");
        when(jwtUtil.toRedisRefreshTokenKey("user-1")).thenReturn("user-1_refreshToken");
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("user-1_refreshToken")).thenReturn("refresh");
        when(redisTemplate.delete("user-1_refreshToken")).thenReturn(true);

        filter.doFilter(request, response, mock(FilterChain.class));

        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("성공 로그아웃은 200 응답을 쓴다")
    void writeLogoutSuccessMessageInResponse_writesOk() throws Exception {
        CustomLogoutFilter filter = new CustomLogoutFilter(
                mock(RedisTemplate.class),
                mock(JwtUtil.class),
                mock(CookieUtil.class),
                new ObjectMapper()
        );
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.writeLogoutSuccessMessageInResponse(response);

        assertThat(response.getStatus()).isEqualTo(200);
    }
}
