package com.project.ds_helper.domain.notification.controller;

import com.project.ds_helper.common.dto.response.ResponseVo;
import com.project.ds_helper.common.enums.SuccessCode;
import com.project.ds_helper.common.enums.SwaggerTagName;
import com.project.ds_helper.domain.notification.dto.request.RegisterPushTokenRequestDto;
import com.project.ds_helper.domain.notification.dto.response.PushTokenResponseDto;
import com.project.ds_helper.domain.notification.service.PushTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = SwaggerTagName.NOTIFICATION)
public class PushTokenController {

    private final PushTokenService pushTokenService;

    @Operation(summary = "푸시 토큰 등록", description = "웹 또는 앱에서 발급받은 푸시 토큰을 현재 사용자 기준으로 등록합니다.")
    @PostMapping("/notification-tokens")
    public ResponseEntity<ResponseVo<PushTokenResponseDto>> registerPushToken(
            Authentication authentication,
            @RequestBody @Valid RegisterPushTokenRequestDto dto
    ) {
        // 1. 클라이언트가 전달한 플랫폼/디바이스/토큰 정보를 현재 사용자 기준으로 등록한다.
        log.debug("PushTokenController.registerPushToken called. platform={}, deviceType={}", dto.platform(), dto.deviceType());
        PushTokenResponseDto responseDto = pushTokenService.registerPushToken(authentication, dto);

        // 2. 등록된 토큰의 식별자와 활성 상태를 응답으로 반환한다.
        ResponseVo<PushTokenResponseDto> responseVo =
                new ResponseVo<>(true, SuccessCode.CREATED, SuccessCode.CREATED.getMessage(), responseDto);
        return ResponseEntity.status(SuccessCode.CREATED.getHttpStatus()).body(responseVo);
    }

    @Operation(summary = "푸시 토큰 비활성화", description = "로그아웃 또는 권한 철회 시 현재 사용자의 푸시 토큰을 비활성화합니다.")
    @DeleteMapping("/notification-tokens/{pushTokenId}")
    public ResponseEntity<ResponseVo<Void>> deactivatePushToken(
            Authentication authentication,
            @PathVariable String pushTokenId
    ) {
        // 1. 현재 로그인한 사용자의 토큰인지 검증한 뒤 발송 대상에서 제외한다.
        log.debug("PushTokenController.deactivatePushToken called. pushTokenId={}", pushTokenId);
        pushTokenService.deactivatePushToken(authentication, pushTokenId);

        // 2. 비활성화 성공 후에는 별도 데이터 없이 공통 성공 응답만 반환한다.
        ResponseVo<Void> responseVo =
                new ResponseVo<>(true, SuccessCode.NO_CONTENT, SuccessCode.NO_CONTENT.getMessage(), null);
        return ResponseEntity.status(SuccessCode.NO_CONTENT.getHttpStatus()).body(responseVo);
    }
}
