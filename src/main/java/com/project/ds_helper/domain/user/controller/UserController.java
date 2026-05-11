package com.project.ds_helper.domain.user.controller;

import com.project.ds_helper.common.enums.SwaggerTagName;
import com.project.ds_helper.domain.user.dto.request.OauthWithdrawRequestDto;
import com.project.ds_helper.domain.user.dto.request.UpdateMyInfoRequestDto;
import com.project.ds_helper.domain.user.dto.response.UserGetSelfInfoAtMyPageResponseDto;
import com.project.ds_helper.domain.user.dto.response.UserIdentifierResponseDto;
import com.project.ds_helper.domain.user.service.GoogleOAuthService;
import com.project.ds_helper.domain.user.service.KakaoOauthService;
import com.project.ds_helper.domain.user.service.NaverOauthService;
import com.project.ds_helper.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
@RequestMapping("/user")
@Tag(name = SwaggerTagName.USER)
@Slf4j
public class UserController {

    private final UserService userService;
    private final KakaoOauthService kakaoOauthService;
    private final GoogleOAuthService googleOAuthService;
    private final NaverOauthService naverOauthService;

    @Operation(summary = "내 정보 조회 (JWT 인증 필요)", description = "마이페이지에서 로그인한 사용자가 자신의 정보를 조회합니다.")
    @GetMapping("/my-info")
    public ResponseEntity<UserGetSelfInfoAtMyPageResponseDto> getMyInfo(Authentication authentication) {
        return ResponseEntity.ok(userService.getMyInfo(authentication));
    }

    @Operation(summary = "현재 유저 식별자 조회 (JWT 인증 필요)", description = "Access token 기반으로 현재 로그인한 사용자의 식별자를 조회합니다.")
    @GetMapping("/my-identifier")
    public ResponseEntity<UserIdentifierResponseDto> getMyIdentifier(Authentication authentication) {
        return ResponseEntity.ok(userService.getMyIdentifier(authentication));
    }

    @Operation(summary = "내 정보 수정 (JWT 인증 필요)", description = "마이페이지에서 로그인한 사용자가 자신의 정보를 수정합니다.")
    @PatchMapping(value = "/my-info", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserGetSelfInfoAtMyPageResponseDto> updateMyInfo(
            Authentication authentication,
            @Parameter(
                    description = "내 정보 수정 DTO",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UpdateMyInfoRequestDto.class)
                    )
            )
            @RequestPart("dto") @Valid UpdateMyInfoRequestDto dto,
            @Parameter(description = "프로필 이미지 파일")
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage
    ) throws Exception {
        return ResponseEntity.ok(userService.updateMyInfo(authentication, dto, profileImage));
    }

    @Operation(summary = "카카오 OAuth 회원 탈퇴 (JWT 인증 필요)")
    @DeleteMapping("/oauth/kakao")
    public ResponseEntity<Void> withdrawKakaoOauthUser(
            Authentication authentication,
            @RequestBody @Valid OauthWithdrawRequestDto dto
    ) {
        kakaoOauthService.withdraw(authentication, dto);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "구글 OAuth 회원 탈퇴 (JWT 인증 필요)")
    @DeleteMapping("/oauth/google")
    public ResponseEntity<Void> withdrawGoogleOauthUser(
            Authentication authentication,
            @RequestBody @Valid OauthWithdrawRequestDto dto
    ) {
        googleOAuthService.withdraw(authentication, dto);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "네이버 OAuth 회원 탈퇴 (JWT 인증 필요)")
    @DeleteMapping("/oauth/naver")
    public ResponseEntity<Void> withdrawNaverOauthUser(
            Authentication authentication,
            @RequestBody @Valid OauthWithdrawRequestDto dto
    ) {
        naverOauthService.withdraw(authentication, dto);
        return ResponseEntity.ok().build();
    }
}
