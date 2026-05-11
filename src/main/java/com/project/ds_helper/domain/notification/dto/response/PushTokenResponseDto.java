package com.project.ds_helper.domain.notification.dto.response;

import com.project.ds_helper.domain.notification.entity.PushToken;
import com.project.ds_helper.domain.notification.enums.PushPlatform;
import com.project.ds_helper.domain.notification.enums.PushTokenDeviceType;

import java.time.LocalDateTime;

public record PushTokenResponseDto(
        String pushTokenId,
        PushPlatform platform,
        PushTokenDeviceType deviceType,
        boolean isActive,
        LocalDateTime lastSeenAt
) {
    public static PushTokenResponseDto toDto(PushToken pushToken) {
        return new PushTokenResponseDto(
                pushToken.getId(),
                pushToken.getPlatform(),
                pushToken.getDeviceType(),
                pushToken.isActive(),
                pushToken.getLastSeenAt()
        );
    }
}
