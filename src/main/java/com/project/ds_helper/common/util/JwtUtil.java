package com.project.ds_helper.common.util;

import com.project.ds_helper.common.enums.JwtTokenType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@Slf4j
public class JwtUtil {

    private final Long ACCESS_TOKEN_EXPIRATION_TIME = 1000 * 60 * 60L;
    private final Long REFRESH_TOKEN_EXPIRATION_TIME = 1000 * 60 * 60 * 24L;

    private final String ACCESS_SECRET = "ThIsIsNoTsEcReTkEyAcCeSsThIsIsNoTsEcReTkEyAcCeSs";
    private final String REFRESH_SECRET = "ThIsIsNoTsEcReTkEyReFrEsHThIsIsNoTsEcReTkEyReFrEsH";

    private final SecretKey ACCESS_SECRET_KEY;
    private final SecretKey REFRESH_SECRET_KEY;

    private final String ACCESS_TOKEN_NAME = "accessToken";
    private final String REFRESH_TOKEN_NAME = "refreshToken";

    public JwtUtil() {
        this.ACCESS_SECRET_KEY = new SecretKeySpec(
                ACCESS_SECRET.getBytes(StandardCharsets.UTF_8),
                Jwts.SIG.HS256.key().build().getAlgorithm()
        );
        this.REFRESH_SECRET_KEY = new SecretKeySpec(
                REFRESH_SECRET.getBytes(StandardCharsets.UTF_8),
                Jwts.SIG.HS256.key().build().getAlgorithm()
        );
    }

    public boolean isExpired(String token) {
        return extractClaims(token)
                .getExpiration()
                .before(new Date());
    }

    public String getId(String token) {
        return extractClaims(token).get("id", String.class);
    }

    public String getEmail(String token) {
        return extractClaims(token).get("email", String.class);
    }

    public String getRole(String token) {
        return extractClaims(token).get("role", String.class);
    }

    public String getType(String token) {
        return extractClaims(token).get("type", String.class);
    }

    public String getTokenType(String token) {
        return extractClaims(token).get("tokenType", String.class);
    }

    public boolean isAccessToken(String token) {
        return isTokenType(token, JwtTokenType.ACCESS_TOKEN_NAME);
    }

    public boolean isRefreshToken(String token) {
        return isTokenType(token, JwtTokenType.REFRESH_TOKEN_NAME);
    }

    public String generateAccessToken(String userId, String userRole, String userType) {
        return Jwts.builder()
                .claim("id", userId)
                .claim("role", userRole)
                .claim("type", userType)
                .claim("tokenType", JwtTokenType.ACCESS_TOKEN_NAME.getTokenName())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION_TIME))
                .signWith(ACCESS_SECRET_KEY)
                .compact();
    }

    public String generateRefreshToken(String userId, String userRole, String userType) {
        return Jwts.builder()
                .claim("id", userId)
                .claim("role", userRole)
                .claim("type", userType)
                .claim("tokenType", JwtTokenType.REFRESH_TOKEN_NAME.getTokenName())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION_TIME))
                .signWith(REFRESH_SECRET_KEY)
                .compact();
    }

    public String toRedisRefreshTokenKey(String userId) {
        return String.format("%s_%s", userId, JwtTokenType.REFRESH_TOKEN_NAME.getTokenName());
    }

    public String getAccessTokenFromRequestHeader(HttpServletRequest httpServletRequest) {
        String bearerToken = httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION);
        log.debug("bearerToken : {}", bearerToken);

        if (bearerToken == null) {
            return null;
        }
        if (bearerToken.isBlank()) {
            return null;
        }
        if (!bearerToken.startsWith("Bearer ")) {
            return null;
        }

        return bearerToken.split(" ")[1];
    }

    public String getRefreshTokenFromRequestHeader(HttpServletRequest httpServletRequest) {
        String refreshToken = httpServletRequest.getHeader(JwtTokenType.REFRESH_TOKEN_NAME.getTokenName());
        log.debug("refreshToken : {}", refreshToken);

        if (refreshToken == null) {
            return null;
        }
        if (refreshToken.isBlank()) {
            return null;
        }

        return refreshToken;
    }

    public Long getAccessTokenExpirationTime() {
        return ACCESS_TOKEN_EXPIRATION_TIME;
    }

    public Long getRefreshTokenExpirationTime() {
        return REFRESH_TOKEN_EXPIRATION_TIME;
    }

    private Claims extractClaims(String token) {
        try {
            return parseClaims(token, ACCESS_SECRET_KEY);
        } catch (JwtException accessTokenException) {
            return parseClaims(token, REFRESH_SECRET_KEY);
        }
    }

    private boolean isTokenType(String token, JwtTokenType expectedType) {
        try {
            String tokenType = getTokenType(token);
            return expectedType.getTokenName().equals(tokenType);
        } catch (Exception exception) {
            return false;
        }
    }

    private Claims parseClaims(String token, SecretKey secretKey) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
