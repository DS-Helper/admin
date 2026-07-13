package com.project.ds_helper.common.filter;

import com.project.ds_helper.common.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JwtFilterTest {

    @Test
    @DisplayName("토큰이 없으면 다음 필터만 호출한다")
    void doFilterInternal_skipsWhenTokenMissing() throws Exception {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        JwtFilter filter = new JwtFilter(jwtUtil);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

        verify(chain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("유효한 access token이면 인증 컨텍스트를 저장한다")
    void doFilterInternal_setsAuthentication() throws Exception {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        JwtFilter filter = new JwtFilter(jwtUtil);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer access");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        when(jwtUtil.getAccessTokenFromRequestHeader(request)).thenReturn("access");
        when(jwtUtil.isAccessToken("access")).thenReturn(true);
        when(jwtUtil.isExpired("access")).thenReturn(false);
        when(jwtUtil.getId("access")).thenReturn("user-1");
        when(jwtUtil.getRole("access")).thenReturn("USER");

        filter.doFilterInternal(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("토큰 파싱 중 예외가 나면 컨텍스트를 비운다")
    void doFilterInternal_clearsContextWhenJwtExceptionOccurs() throws Exception {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        JwtFilter filter = new JwtFilter(jwtUtil);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer access");
        FilterChain chain = mock(FilterChain.class);

        when(jwtUtil.getAccessTokenFromRequestHeader(request)).thenReturn("access");
        when(jwtUtil.isAccessToken("access")).thenReturn(true);
        when(jwtUtil.isExpired("access")).thenReturn(false);
        when(jwtUtil.getId("access")).thenThrow(new JwtException("boom"));

        filter.doFilterInternal(request, new MockHttpServletResponse(), chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
