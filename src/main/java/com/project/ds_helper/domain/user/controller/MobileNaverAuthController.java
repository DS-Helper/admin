package com.project.ds_helper.domain.user.controller;

import com.project.ds_helper.common.dto.response.JwtResponse;
import com.project.ds_helper.common.dto.response.ResponseVo;
import com.project.ds_helper.common.enums.SuccessCode;
import com.project.ds_helper.common.enums.SwaggerTagName;
import com.project.ds_helper.domain.user.dto.request.MobileNaverLoginRequestDto;
import com.project.ds_helper.domain.user.service.MobileNaverOauthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/mobile")
@Tag(name = SwaggerTagName.APP_AUTH)
public class MobileNaverAuthController {

    private final MobileNaverOauthService mobileNaverOauthService;

    @Operation(summary = "모바일 네이버 로그인", description = "모바일 네이버 access token을 받아 로그인 또는 회원가입을 처리합니다.")
    @PostMapping("/oauth/naver/login")
    public ResponseEntity<ResponseVo<JwtResponse>> mobileNaverLogin(@RequestBody @Valid MobileNaverLoginRequestDto dto,
                                                       HttpServletResponse httpServletResponse) throws IOException {
        JwtResponse jwtResponse = mobileNaverOauthService.mobileNaverLogin(dto, httpServletResponse);
        ResponseVo<JwtResponse> responseVo = new ResponseVo<>(true, SuccessCode.OK, SuccessCode.OK.getMessage(), jwtResponse);
        return new ResponseEntity<>(responseVo, responseVo.getCode().httpStatus());
    }
}
