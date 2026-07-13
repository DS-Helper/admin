package com.project.ds_helper.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ds_helper.common.filter.CustomLoginFilter;
import com.project.ds_helper.common.filter.CustomLogoutFilter;
import com.project.ds_helper.common.filter.JwtFilter;
import com.project.ds_helper.common.util.CookieUtil;
import com.project.ds_helper.common.util.JwtUtil;
import com.project.ds_helper.domain.user.repository.UserRepository;
import com.project.ds_helper.domain.user.service.UserLoginHistoryService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity(debug = false)
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final CookieUtil cookieUtil;
    private final StringRedisTemplate stringRedisTemplate;
    private final AuthenticationConfiguration authenticationConfiguration;
    private final ObjectMapper objectMapper;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final UserRepository userRepository;
    private final UserLoginHistoryService userLoginHistoryService;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    public SecurityConfig(
            JwtUtil jwtUtil,
            CookieUtil cookieUtil,
            StringRedisTemplate stringRedisTemplate,
            AuthenticationConfiguration authenticationConfiguration,
            @Qualifier("customObjectMapper") ObjectMapper objectMapper,
            BCryptPasswordEncoder bCryptPasswordEncoder,
            UserRepository userRepository,
            UserLoginHistoryService userLoginHistoryService,
            CustomAuthenticationEntryPoint customAuthenticationEntryPoint,
            CustomAccessDeniedHandler customAccessDeniedHandler
    ) {
        this.jwtUtil = jwtUtil;
        this.cookieUtil = cookieUtil;
        this.stringRedisTemplate = stringRedisTemplate;
        this.authenticationConfiguration = authenticationConfiguration;
        this.objectMapper = objectMapper;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.userRepository = userRepository;
        this.userLoginHistoryService = userLoginHistoryService;
        this.customAuthenticationEntryPoint = customAuthenticationEntryPoint;
        this.customAccessDeniedHandler = customAccessDeniedHandler;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable);
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
        http.formLogin(AbstractHttpConfigurer::disable);
        http.httpBasic(AbstractHttpConfigurer::disable);
        http.logout(AbstractHttpConfigurer::disable);
        http.exceptionHandling(exception -> exception
                .authenticationEntryPoint(customAuthenticationEntryPoint)
                .accessDeniedHandler(customAccessDeniedHandler)
        );

        http.addFilterBefore(new JwtFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class);
        http.addFilterAt(
                new CustomLoginFilter(
                        getAuthenticationManager(authenticationConfiguration),
                        jwtUtil,
                        cookieUtil,
                        bCryptPasswordEncoder,
                        stringRedisTemplate,
                        userRepository,
                        userLoginHistoryService,
                        objectMapper
                ),
                UsernamePasswordAuthenticationFilter.class
        );
        http.addFilterBefore(
                new CustomLogoutFilter(stringRedisTemplate, jwtUtil, cookieUtil, objectMapper),
                LogoutFilter.class
        );

        http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/check-logged-in").permitAll()
                .requestMatchers("/health-check").permitAll()
                .requestMatchers("/test/**").permitAll()
                .requestMatchers("/admin/**").hasAuthority("ADMIN")
                .requestMatchers("/api/v1/admin/volunteer/**").hasAuthority("ADMIN")
                // 쓰레기통 기준 데이터와 이미지는 운영 데이터이므로 관리자만 업로드할 수 있다.
                .requestMatchers(HttpMethod.POST, "/trash-bins/upload", "/trash-bins/images").hasAuthority("ADMIN")
                .requestMatchers(
                        "/oauth/**",
                        "/trash-bins/**",
                        "/api/v1/mobile/oauth/**",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/swagger/**"
                ).permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/join/organization").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/join/user").permitAll()
                .requestMatchers(HttpMethod.POST, "/auth/login/organization").permitAll()
                .requestMatchers(HttpMethod.GET, "/posts", "/posts/*", "/boards", "/board/*", "/comments/*/comments", "/comments/*/children").permitAll()
                .anyRequest().authenticated()
        );

        return http.build();
    }

    @Bean
    UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of(
                "*",
                "http://localhost:3000", "http://localhost:5173", "http://localhost:8080",
                "https://testmy-netlify.app", "https://test.dshelper.kro.kr",
                "https://client.dshelper.kro.kr",
                "https://dshelper.netlify.app",
                "https://www.dshelper.kr", "https://server.dshelper.kr",
                "https://test.dshelper.kr"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    AuthenticationManager getAuthenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
