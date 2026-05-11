package com.project.ds_helper.domain.notification.controller;

import com.project.ds_helper.common.dto.response.ResponseVo;
import com.project.ds_helper.domain.notification.dto.request.RegisterPushTokenRequestDto;
import com.project.ds_helper.domain.notification.dto.response.PushTokenResponseDto;
import com.project.ds_helper.domain.notification.enums.PushPlatform;
import com.project.ds_helper.domain.notification.enums.PushTokenDeviceType;
import com.project.ds_helper.domain.notification.service.PushTokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PushTokenControllerTest {

    @Mock
    private PushTokenService pushTokenService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private PushTokenController pushTokenController;

    @Test
    @DisplayName("푸시 토큰 등록 시 생성 응답과 토큰 정보를 반환한다")
    void registerPushToken_returnsCreatedResponse() {
        RegisterPushTokenRequestDto dto = new RegisterPushTokenRequestDto(
                PushPlatform.WEB,
                PushTokenDeviceType.CHROME,
                "token-value"
        );
        PushTokenResponseDto responseDto = new PushTokenResponseDto(
                "push-token-1",
                PushPlatform.WEB,
                PushTokenDeviceType.CHROME,
                true,
                null
        );

        when(pushTokenService.registerPushToken(authentication, dto)).thenReturn(responseDto);

        ResponseEntity<ResponseVo<PushTokenResponseDto>> response =
                pushTokenController.registerPushToken(authentication, dto);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().pushTokenId()).isEqualTo("push-token-1");
    }

    @Test
    @DisplayName("푸시 토큰 비활성화 시 no content 응답을 반환한다")
    void deactivatePushToken_returnsNoContentResponse() {
        doNothing().when(pushTokenService).deactivatePushToken(authentication, "push-token-1");

        ResponseEntity<ResponseVo<Void>> response =
                pushTokenController.deactivatePushToken(authentication, "push-token-1");

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
    }
}
