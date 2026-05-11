package com.project.ds_helper.domain.user.controller;

import com.project.ds_helper.common.dto.response.JwtResponse;
import com.project.ds_helper.common.dto.response.ResponseVo;
import com.project.ds_helper.common.enums.SuccessCode;
import com.project.ds_helper.common.enums.SwaggerTagName;
import com.project.ds_helper.domain.user.dto.request.KakaoCodeLoginRequestDto;
import com.project.ds_helper.domain.user.service.KakaoOauthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/oauth/kakao")
@Tag(name = SwaggerTagName.AUTH)
public class KakaoAuthController {

    private final KakaoOauthService kakaoOauthService;

    @Operation(summary = "카카오 로그인 URL 조회", description = "카카오 OAuth 로그인 페이지로 이동하기 위한 URL을 조회합니다.")
    @GetMapping("/login-url")
    public ResponseEntity<String> getKakaoLoginUrl(HttpServletResponse response) throws IOException {
        return ResponseEntity.ok(kakaoOauthService.getKakaoLoginUrl());
    }

    @Operation(summary = "카카오 로그인", description = "카카오 authorization code를 받아 사용자 정보 조회 후 로그인 또는 회원가입을 처리합니다.")
    @PostMapping("/login")
    public ResponseEntity<ResponseVo<JwtResponse>> kakaoLogin(
            @RequestBody @Valid KakaoCodeLoginRequestDto dto
    ) throws IOException {
        JwtResponse jwtResponse = kakaoOauthService.kakaoLogin(dto.code());
        ResponseVo<JwtResponse> responseVo = new ResponseVo<>(true, SuccessCode.OK, SuccessCode.OK.getMessage(), jwtResponse);
        return new ResponseEntity<>(responseVo, responseVo.getCode().httpStatus());
    }
}
