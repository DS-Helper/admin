package com.project.ds_helper.domain.notification.service;

import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.notification.dto.request.RegisterPushTokenRequestDto;
import com.project.ds_helper.domain.notification.dto.response.PushTokenResponseDto;
import com.project.ds_helper.domain.notification.entity.PushToken;
import com.project.ds_helper.domain.notification.enums.PushPlatform;
import com.project.ds_helper.domain.notification.enums.PushTokenDeviceType;
import com.project.ds_helper.domain.notification.repository.PushTokenRepository;
import com.project.ds_helper.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PushTokenServiceTest {

    @Mock
    private PushTokenRepository pushTokenRepository;

    @Mock
    private UserUtil userUtil;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private PushTokenService pushTokenService;

    @Test
    @DisplayName("푸시 토큰 등록 시 기존 토큰이 없으면 새 엔티티를 저장한다")
    void registerPushToken_savesNewToken() {
        User user = User.builder().id("user-1").name("홍길동").build();
        RegisterPushTokenRequestDto dto = new RegisterPushTokenRequestDto(
                PushPlatform.WEB,
                PushTokenDeviceType.CHROME,
                "token-value"
        );

        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(userUtil.findUserById("user-1")).thenReturn(user);
        when(pushTokenRepository.findByUser_IdAndToken("user-1", "token-value")).thenReturn(Optional.empty());
        when(pushTokenRepository.save(any(PushToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PushTokenResponseDto result = pushTokenService.registerPushToken(authentication, dto);

        verify(pushTokenRepository).save(any(PushToken.class));
        assertThat(result.platform()).isEqualTo(PushPlatform.WEB);
        assertThat(result.deviceType()).isEqualTo(PushTokenDeviceType.CHROME);
        assertThat(result.isActive()).isTrue();
    }

    @Test
    @DisplayName("푸시 토큰 등록 시 기존 토큰이 있으면 갱신한다")
    void registerPushToken_refreshesExistingToken() {
        User user = User.builder().id("user-1").name("홍길동").build();
        PushToken existing = PushToken.builder()
                .id("push-token-1")
                .user(user)
                .platform(PushPlatform.ANDROID)
                .deviceType(PushTokenDeviceType.APP_ANDROID)
                .token("token-value")
                .build();
        RegisterPushTokenRequestDto dto = new RegisterPushTokenRequestDto(
                PushPlatform.WEB,
                PushTokenDeviceType.CHROME,
                "token-value"
        );

        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(userUtil.findUserById("user-1")).thenReturn(user);
        when(pushTokenRepository.findByUser_IdAndToken("user-1", "token-value")).thenReturn(Optional.of(existing));
        when(pushTokenRepository.save(any(PushToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PushTokenResponseDto result = pushTokenService.registerPushToken(authentication, dto);

        assertThat(result.platform()).isEqualTo(PushPlatform.WEB);
        assertThat(result.deviceType()).isEqualTo(PushTokenDeviceType.CHROME);
        verify(pushTokenRepository).save(existing);
    }

    @Test
    @DisplayName("푸시 토큰 비활성화 시 현재 사용자 토큰만 비활성화한다")
    void deactivatePushToken_deactivatesOwnedToken() {
        PushToken pushToken = PushToken.builder()
                .id("push-token-1")
                .user(User.builder().id("user-1").build())
                .platform(PushPlatform.WEB)
                .deviceType(PushTokenDeviceType.CHROME)
                .token("token-value")
                .build();

        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(pushTokenRepository.findByIdAndUser_Id("push-token-1", "user-1")).thenReturn(Optional.of(pushToken));

        pushTokenService.deactivatePushToken(authentication, "push-token-1");

        assertThat(pushToken.isActive()).isFalse();
        assertThat(pushToken.getLastSeenAt()).isNotNull();
    }

    @Test
    @DisplayName("푸시 토큰 비활성화 시 토큰이 없으면 예외가 발생한다")
    void deactivatePushToken_throwsWhenMissing() {
        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(pushTokenRepository.findByIdAndUser_Id("push-token-1", "user-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pushTokenService.deactivatePushToken(authentication, "push-token-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Push Token Not Found");
    }

    @Test
    @DisplayName("활성 푸시 토큰 조회 시 활성 토큰 목록만 반환한다")
    void getActivePushTokensByUserIds_returnsActiveTokens() {
        PushToken pushToken = PushToken.builder()
                .id("push-token-1")
                .user(User.builder().id("user-1").build())
                .platform(PushPlatform.WEB)
                .deviceType(PushTokenDeviceType.CHROME)
                .token("token-value")
                .build();

        when(pushTokenRepository.findAllByUser_IdInAndIsActiveTrue(List.of("user-1")))
                .thenReturn(List.of(pushToken));

        List<PushToken> result = pushTokenService.getActivePushTokensByUserIds(List.of("user-1"));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo("push-token-1");
    }

    @Test
    @DisplayName("활성 푸시 토큰 조회는 빈 userIds면 빈 목록을 반환한다")
    void getActivePushTokensByUserIds_returnsEmptyWhenEmptyUserIds() {
        List<PushToken> result = pushTokenService.getActivePushTokensByUserIds(List.of());

        assertThat(result).isEmpty();
        verify(pushTokenRepository, never()).findAllByUser_IdInAndIsActiveTrue(any());
    }
}
