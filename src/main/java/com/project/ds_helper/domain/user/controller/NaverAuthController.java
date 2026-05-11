package com.project.ds_helper.domain.user.controller;

import com.project.ds_helper.common.dto.response.JwtResponse;
import com.project.ds_helper.common.dto.response.ResponseVo;
import com.project.ds_helper.common.enums.SuccessCode;
import com.project.ds_helper.common.enums.SwaggerTagName;
import com.project.ds_helper.domain.user.dto.request.NaverCodeLoginRequestDto;
import com.project.ds_helper.domain.user.service.NaverOauthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/oauth/naver")
@RequiredArgsConstructor
@Tag(name = SwaggerTagName.AUTH)
public class NaverAuthController {

    private final NaverOauthService naverOauthService;

    @Operation(summary = "네이버 로그인 URL 조회", description = "네이버 OAuth 로그인 페이지로 이동하기 위한 URL을 조회합니다.")
    @GetMapping("/login-url")
    public ResponseEntity<String> naverLoginUrl() {
        return ResponseEntity.ok(naverOauthService.naverLoginUrl());
    }

    @Operation(summary = "네이버 로그인", description = "네이버 authorization code와 state를 받아 사용자 정보 조회 후 로그인 또는 회원가입을 처리합니다.")
    @PostMapping("/login")
    public ResponseEntity<ResponseVo<JwtResponse>> naverLogin(
            @RequestBody @Valid NaverCodeLoginRequestDto dto
    ) throws IOException {
        JwtResponse jwtResponse = naverOauthService.naverLogin(dto.code(), dto.state());
        ResponseVo<JwtResponse> responseVo = new ResponseVo<>(true, SuccessCode.OK, SuccessCode.OK.getMessage(), jwtResponse);
        return new ResponseEntity<>(responseVo, responseVo.getCode().httpStatus());
    }
}
