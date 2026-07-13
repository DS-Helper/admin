package com.project.ds_helper.domain.user.service;

import com.project.ds_helper.common.enums.JwtTokenType;
import com.project.ds_helper.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletResponse;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserAuthTokenService {

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;

    public void generateJwtTokenAndPutInResponseHeader(HttpServletResponse httpServletResponse, String userId, String userRole, String userType) {
        String accessToken = jwtUtil.generateAccessToken(userId, userRole, userType);
        String refreshToken = jwtUtil.generateRefreshToken(userId, userRole, userType);
        log.debug("accessToken : {}, refreshToken : {}", accessToken, refreshToken);

        stringRedisTemplate.opsForValue().set(jwtUtil.toRedisRefreshTokenKey(userId), refreshToken);
        log.debug("RefreshToken Saved. UserId : {}, key : {}", userId, jwtUtil.toRedisRefreshTokenKey(userId));

        httpServletResponse.setHeader(JwtTokenType.ACCESS_TOKEN_NAME.getTokenName(), accessToken);
        httpServletResponse.setHeader(JwtTokenType.REFRESH_TOKEN_NAME.getTokenName(), refreshToken);
        log.debug("jwt token is put in header");
    }

    public Object checkIfUserLoggedIn(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return false;
        }
        if (jwtUtil.isExpired(refreshToken)) {
            return false;
        }
        String userId = jwtUtil.getId(refreshToken);
        log.debug("userID : {}", userId);

        String refreshTokenFromRedis = stringRedisTemplate.opsForValue().get(jwtUtil.toRedisRefreshTokenKey(userId));
        log.debug("refreshTokenFromRedis : {}", refreshTokenFromRedis);
        if (refreshTokenFromRedis == null || refreshTokenFromRedis.isBlank() || jwtUtil.isExpired(refreshTokenFromRedis)) {
            log.debug("refreshToken from redis is null or blank or expired");
            return false;
        }

        return true;
    }
}
