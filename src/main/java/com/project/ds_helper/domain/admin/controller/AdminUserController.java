package com.project.ds_helper.domain.admin.controller;

import com.project.ds_helper.common.enums.SwaggerTagName;
import com.project.ds_helper.domain.admin.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Slf4j
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Tag(name = SwaggerTagName.ADMIN_DASHBOARD)
    @Operation(summary = "가입된 유저 수 조회 (JWT 인증 필요)", description = "가입된 유저 수를 조회합니다.")
    @GetMapping("/user-count")
    public ResponseEntity<?> userCount(Authentication authentication) {
        log.debug("AdminUserController.userCount called.");
        return ResponseEntity.ok(adminUserService.userCount(authentication));
    }

    @Tag(name = SwaggerTagName.ADMIN_DASHBOARD)
    @Operation(summary = "이름 기반 유저 정보 조회 (JWT 인증 필요)", description = "이름 기반으로 유저 정보를 조회합니다.")
    @GetMapping("/user-info/{name}")
    public ResponseEntity<?> getUserInfoByName(@RequestParam(value = "name") String name, Authentication authentication) {
        log.debug("AdminUserController.getUserInfoByName called. name={}", name);
        return ResponseEntity.ok(adminUserService.getUserInfoByName(name, authentication));
    }
}
