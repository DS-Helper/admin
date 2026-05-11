package com.project.ds_helper.common.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ds_helper.common.dto.response.ResponseVo;
import com.project.ds_helper.common.enums.ErrorCode;
import com.project.ds_helper.common.enums.SuccessCode;
import com.project.ds_helper.common.util.CookieUtil;
import com.project.ds_helper.common.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
public class CustomLogoutFilter extends OncePerRequestFilter {

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtUtil jwtUtil;
    private final CookieUtil cookieUtil;
    private final ObjectMapper objectMapper;

    @Value("${client.url}")
    private String clientUrl;

    public CustomLogoutFilter(
            @Qualifier("CustomStringRedisTemplate") RedisTemplate<String, String> redisTemplate,
            JwtUtil jwtUtil,
            CookieUtil cookieUtil,
            ObjectMapper objectMapper
    ) {
        this.redisTemplate = redisTemplate;
        this.jwtUtil = jwtUtil;
        this.cookieUtil = cookieUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        doFilter(request, response, filterChain);
    }

    public void doFilter(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain)
            throws ServletException, IOException {

        if (!UrlFilter.checkIfLogoutPath(httpServletRequest.getRequestURI())) {
            filterChain.doFilter(httpServletRequest, httpServletResponse);
            return;
        }
        log.debug("Is Logout URL");

        String refreshToken = jwtUtil.getRefreshTokenFromRequestHeader(httpServletRequest);

        if (refreshToken == null || refreshToken.isBlank() || jwtUtil.isExpired(refreshToken)) {
            writeUnauthorizedResponse(httpServletResponse);
            return;
        }
        if (!jwtUtil.isRefreshToken(refreshToken)) {
            writeUnauthorizedResponse(httpServletResponse);
            return;
        }

        String userId = jwtUtil.getId(refreshToken);
        log.debug("logout userId : {}", userId);

        String refreshTokenRedisKey = jwtUtil.toRedisRefreshTokenKey(userId);
        String refreshTokenInRedis = redisTemplate.opsForValue().get(refreshTokenRedisKey);

        if (refreshTokenInRedis == null || refreshTokenInRedis.isBlank()) {
            writeUnauthorizedResponse(httpServletResponse);
            return;
        }

        if (!redisTemplate.delete(jwtUtil.toRedisRefreshTokenKey(userId))) {
            writeUnauthorizedResponse(httpServletResponse);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            SecurityContextHolder.clearContext();
        }

        writeLogoutSuccessMessageInResponse(httpServletResponse);
    }

    private void writeUnauthorizedResponse(HttpServletResponse httpServletResponse) throws IOException {
        log.debug("Is Not Logged In User");
        httpServletResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
        httpServletResponse.setCharacterEncoding("UTF-8");
        httpServletResponse.setStatus(ErrorCode.UNAUTHORIZED.getHttpStatus().value());
        objectMapper.writeValue(
                httpServletResponse.getWriter(),
                ResponseVo.error(ErrorCode.UNAUTHORIZED, ErrorCode.UNAUTHORIZED.getMessage())
        );
    }

    public void writeLogoutSuccessMessageInResponse(HttpServletResponse httpServletResponse) throws IOException {
        httpServletResponse.setStatus(200);
        httpServletResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
        httpServletResponse.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(
                httpServletResponse.getWriter(),
                new ResponseVo<>(true, SuccessCode.OK, "Logout successful", null)
        );
    }
}
