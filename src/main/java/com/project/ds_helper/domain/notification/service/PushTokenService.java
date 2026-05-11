package com.project.ds_helper.domain.notification.service;

import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.notification.dto.request.RegisterPushTokenRequestDto;
import com.project.ds_helper.domain.notification.dto.response.PushTokenResponseDto;
import com.project.ds_helper.domain.notification.entity.PushToken;
import com.project.ds_helper.domain.notification.repository.PushTokenRepository;
import com.project.ds_helper.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushTokenService {

    private final PushTokenRepository pushTokenRepository;
    private final UserUtil userUtil;

    @Transactional
    public PushTokenResponseDto registerPushToken(Authentication authentication, RegisterPushTokenRequestDto dto) {
        // 1. 현재 로그인한 사용자 정보를 조회해 토큰 소유자를 확정한다.
        String userId = userUtil.extractUserId(authentication);
        User user = userUtil.findUserById(userId);
        LocalDateTime now = LocalDateTime.now();
        log.debug("PushTokenService.registerPushToken started. userId={}, platform={}, deviceType={}", userId, dto.platform(), dto.deviceType());

        // 2. 같은 유저가 같은 토큰을 이미 등록했다면 활성 상태와 최신 시각만 갱신한다.
        PushToken pushToken = pushTokenRepository.findByUser_IdAndToken(userId, dto.token())
                .map(existingPushToken -> {
                    log.debug("PushTokenService.registerPushToken found existing token. pushTokenId={}", existingPushToken.getId());
                    existingPushToken.refresh(dto.platform(), dto.deviceType(), dto.token(), now);
                    return existingPushToken;
                })
                .orElseGet(() -> {
                    log.debug("PushTokenService.registerPushToken creating new token entity");
                    return PushToken.builder()
                            .user(user)
                            .platform(dto.platform())
                            .deviceType(dto.deviceType())
                            .token(dto.token())
                            .lastSeenAt(now)
                            .build();
                });

        // 3. 최종 토큰 엔티티를 저장하고 API 응답 DTO로 변환한다.
        PushToken savedPushToken = pushTokenRepository.save(pushToken);
        log.debug("PushTokenService.registerPushToken completed. pushTokenId={}, isActive={}", savedPushToken.getId(), savedPushToken.isActive());
        return PushTokenResponseDto.toDto(savedPushToken);
    }

    @Transactional
    public void deactivatePushToken(Authentication authentication, String pushTokenId) {
        // 1. 현재 로그인한 사용자 기준으로 본인 토큰만 비활성화할 수 있게 제한한다.
        String userId = userUtil.extractUserId(authentication);
        log.debug("PushTokenService.deactivatePushToken started. userId={}, pushTokenId={}", userId, pushTokenId);

        PushToken pushToken = pushTokenRepository.findByIdAndUser_Id(pushTokenId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Push Token Not Found"));

        // 2. 토큰을 즉시 비활성화해 이후 발송 대상에서 제외한다.
        pushToken.deactivate(LocalDateTime.now());
        log.debug("PushTokenService.deactivatePushToken completed. pushTokenId={}", pushTokenId);
    }

    @Transactional(readOnly = true)
    public List<PushToken> getActivePushTokensByUserIds(Collection<String> userIds) {
        // 1. 수신자 목록이 비어 있으면 불필요한 조회를 막기 위해 즉시 빈 목록을 반환한다.
        if (userIds == null || userIds.isEmpty()) {
            log.debug("PushTokenService.getActivePushTokensByUserIds skipped. empty userIds");
            return List.of();
        }

        // 2. 활성 토큰만 조회해 실제 발송 가능한 대상만 다음 계층으로 전달한다.
        List<PushToken> pushTokens = pushTokenRepository.findAllByUser_IdInAndIsActiveTrue(userIds);
        log.debug("PushTokenService.getActivePushTokensByUserIds resolved active tokens. userCount={}, tokenCount={}", userIds.size(), pushTokens.size());
        return pushTokens;
    }
}
