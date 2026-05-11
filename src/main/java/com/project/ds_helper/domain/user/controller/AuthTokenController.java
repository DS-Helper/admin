package com.project.ds_helper.domain.user.controller;

import com.project.ds_helper.common.dto.response.JwtResponse;
import com.project.ds_helper.common.dto.response.ResponseCodeInfoDto;
import com.project.ds_helper.common.dto.response.ResponseVo;
import com.project.ds_helper.common.enums.ErrorCode;
import com.project.ds_helper.common.enums.JwtTokenType;
import com.project.ds_helper.common.enums.SuccessCode;
import com.project.ds_helper.common.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@Slf4j
@RequestMapping("/auth-token")
public class AuthTokenController {

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;

    public AuthTokenController(
            JwtUtil jwtUtil,
            @Qualifier(value = "CustomStringRedisTemplate") StringRedisTemplate stringRedisTemplate
    ){
        this.jwtUtil = jwtUtil;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /** refreshToken 재발급 **/
    public ResponseEntity<ResponseVo> reGenerateJWT(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse){

        // 기존 refreshToken 추출
        String oldRefreshToken = jwtUtil.getRefreshTokenFromRequestHeader(httpServletRequest);
        if(oldRefreshToken == null || oldRefreshToken.isBlank()){
            return tokenInvalidResponse();
        }
        if(!jwtUtil.isRefreshToken(oldRefreshToken)){
            return tokenInvalidResponse();
        }

        // 무효한 토큰일 시 리턴
        if(jwtUtil.isExpired(oldRefreshToken)){
            return tokenInvalidResponse();
        }

        // 유저 정보 추출
        String userId = jwtUtil.getId(oldRefreshToken);
        String userRole = jwtUtil.getRole(oldRefreshToken);
        String userType = jwtUtil.getType(oldRefreshToken);

        // Redis에서 토큰 삭제
        if(stringRedisTemplate.delete(jwtUtil.toRedisRefreshTokenKey(userId))){
            return tokenInvalidResponse();
        }

        String accessToken = generateAccessToken(userId, userRole, userType);
        String refreshToken = generateRefreshToken(userId, userRole, userType);
        stringRedisTemplate.opsForValue().set(jwtUtil.toRedisRefreshTokenKey(userId), refreshToken, redisRefreshTokenExpirationTime());
        JwtResponse jwtResponse = new JwtResponse(accessToken, refreshToken);

        ResponseVo responseVo = new ResponseVo(true, SuccessCode.OK, SuccessCode.OK.getMessage(), jwtResponse);
        return new ResponseEntity<>(responseVo, responseVo.getCode().httpStatus());
    }

    /** 토큰 무효 시 응답 생성 **/
    public ResponseEntity<ResponseVo> tokenInvalidResponse(){
        ResponseVo responseVo = new ResponseVo();
        responseVo.setSuccess(false);
        responseVo.setCode(ResponseCodeInfoDto.from(ErrorCode.UNAUTHORIZED));
        responseVo.setMessage(ErrorCode.UNAUTHORIZED.getMessage());
        responseVo.setData(null);
        return new ResponseEntity<>(responseVo, responseVo.getCode().httpStatus());
    }

    /** refresh token의 만료기한을 리턴 **/
    public Duration redisRefreshTokenExpirationTime(){
        return Duration.ofSeconds(jwtUtil.getRefreshTokenExpirationTime());
    }

    /** access token 발급 **/
    public String generateAccessToken(
            String userId,
            String userRole,
            String userType
    ){
        return jwtUtil.generateAccessToken(userId, userRole, userType);
    }

    /** refresh token 발급 **/
    public String generateRefreshToken(
            String userId,
            String userRole,
            String userType
    ){
        return jwtUtil.generateRefreshToken(userId, userRole, userType);
    }
}
