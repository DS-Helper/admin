package com.project.ds_helper.domain.admin.controller;

import com.project.ds_helper.common.dto.response.JwtResponse;
import com.project.ds_helper.domain.admin.dto.request.AdminLoginReqDto;
import com.project.ds_helper.domain.admin.service.AdminAuthService;
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

@RestController
@RequestMapping("/admin/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "관리자 > 인증")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @Operation(summary = "관리자 로그인 API", description = "아이디와 비밀번호를 사용하여 관리자 로그인을 진행합니다.")
    @PostMapping("/login")
    public ResponseEntity<JwtResponse> adminLogin(
            @RequestBody @Valid AdminLoginReqDto dto,
            HttpServletResponse response
    ) {
        log.debug("AdminAuthController.adminLogin called.");
        return ResponseEntity.ok(adminAuthService.adminLogin(dto, response));
    }
}
