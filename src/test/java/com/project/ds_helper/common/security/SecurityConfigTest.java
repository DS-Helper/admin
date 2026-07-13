package com.project.ds_helper.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ds_helper.common.util.CookieUtil;
import com.project.ds_helper.common.util.JwtUtil;
import com.project.ds_helper.domain.user.repository.UserRepository;
import com.project.ds_helper.domain.user.service.UserLoginHistoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.any;
import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {

    @Test
    @DisplayName("CORS 설정 소스와 인증 매니저 빈을 만든다")
    void beans_exist() throws Exception {
        AuthenticationConfiguration authenticationConfiguration = mock(AuthenticationConfiguration.class);
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(authenticationManager);

        SecurityConfig config = new SecurityConfig(
                mock(JwtUtil.class),
                mock(CookieUtil.class),
                mock(StringRedisTemplate.class),
                authenticationConfiguration,
                new ObjectMapper(),
                mock(BCryptPasswordEncoder.class),
                mock(UserRepository.class),
                mock(UserLoginHistoryService.class),
                mock(CustomAuthenticationEntryPoint.class),
                mock(CustomAccessDeniedHandler.class)
        );

        UrlBasedCorsConfigurationSource source = config.corsConfigurationSource();
        assertThat(source).isNotNull();
        assertThat(config.getAuthenticationManager(authenticationConfiguration)).isSameAs(authenticationManager);
        CorsConfiguration cors = source.getCorsConfiguration(new org.springframework.mock.web.MockHttpServletRequest("GET", "/"));
        assertThat(cors).isNotNull();
        assertThat(cors.getAllowedOriginPatterns()).contains("*", "http://localhost:3000");
        assertThat(cors.getAllowedMethods()).contains("GET", "POST", "DELETE");
    }

    @Test
    @DisplayName("필터 체인은 예외 없이 생성된다")
    void filterChain_builds() throws Exception {
        AuthenticationConfiguration authenticationConfiguration = mock(AuthenticationConfiguration.class);
        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(mock(AuthenticationManager.class));
        SecurityConfig config = new SecurityConfig(
                mock(JwtUtil.class),
                mock(CookieUtil.class),
                mock(StringRedisTemplate.class),
                authenticationConfiguration,
                new ObjectMapper(),
                mock(BCryptPasswordEncoder.class),
                mock(UserRepository.class),
                mock(UserLoginHistoryService.class),
                mock(CustomAuthenticationEntryPoint.class),
                mock(CustomAccessDeniedHandler.class)
        );
        HttpSecurity http = mock(HttpSecurity.class, RETURNS_DEEP_STUBS);
        when(http.csrf(any())).thenReturn(http);
        when(http.cors(any())).thenReturn(http);
        when(http.formLogin(any())).thenReturn(http);
        when(http.httpBasic(any())).thenReturn(http);
        when(http.logout(any())).thenReturn(http);
        when(http.exceptionHandling(any())).thenReturn(http);
        when(http.addFilterBefore(any(), any())).thenReturn(http);
        when(http.addFilterAt(any(), any())).thenReturn(http);
        when(http.authorizeHttpRequests(any())).thenReturn(http);
        when(http.build()).thenReturn(mock(DefaultSecurityFilterChain.class));

        assertThat(config.filterChain(http)).isNotNull();
    }
}
