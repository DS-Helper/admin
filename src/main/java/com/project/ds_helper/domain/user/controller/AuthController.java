package com.project.ds_helper.domain.user.controller;

import com.project.ds_helper.common.enums.SwaggerTagName;
import com.project.ds_helper.common.util.CookieUtil;
import com.project.ds_helper.common.util.JwtUtil;
import com.project.ds_helper.domain.user.dto.request.AdminLoginReqDto;
import com.project.ds_helper.domain.user.dto.request.OrganizationJoinReqDto;
import com.project.ds_helper.domain.user.dto.request.UserJoinReqDto;
import com.project.ds_helper.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
@Tag(name = SwaggerTagName.AUTH)
@Slf4j
public class AuthController {

    private final UserService userService;
    private final CookieUtil cookieUtil;
    private final JwtUtil jwtUtil;

    @Operation(summary = "로그인 여부 확인", description = "리프레시 토큰 헤더를 기준으로 현재 사용자의 로그인 여부를 확인합니다.")
    @GetMapping("/check-logged-in")
    public ResponseEntity<?> checkIfUserLoggedIn(HttpServletRequest request) {
        return ResponseEntity.ok(userService.checkIfUserLoggedIn(jwtUtil.getRefreshTokenFromRequestHeader(request)));
    }

    @Operation(summary = "기관 회원가입", description = "기관 회원가입 정보와 인증 파일을 받아 기관 계정을 생성합니다.")
    @PostMapping(value = "/join/organization", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> organizationJoin(
            @Parameter(
                    description = "기관 회원가입 DTO",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = OrganizationJoinReqDto.class)
                    )
            )
            @RequestPart("dto") @Valid OrganizationJoinReqDto dto,
            @Parameter(description = "기관 인증 파일")
            @RequestPart(value = "certifications", required = false) List<MultipartFile> certifications
    ) throws IOException {
        userService.organizationJoin(dto, certifications);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "유저 회원가입", description = "일반 사용자 회원가입 정보를 받아 사용자 계정을 생성합니다.")
    @PostMapping(value = "/join/user")
    public ResponseEntity<?> userJoin(
            @Parameter(
                    description = "유저 회원가입 DTO",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UserJoinReqDto.class)
                    )
            )
            @RequestBody @Valid UserJoinReqDto dto,
            HttpServletResponse httpServletResponse
    ) throws IOException {
        userService.userJoin(dto, httpServletResponse);
        return ResponseEntity.ok().build();
    }

}
