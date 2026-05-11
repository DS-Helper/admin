package com.project.ds_helper.domain.notification.dto.request;

import com.project.ds_helper.domain.notification.enums.PushPlatform;
import com.project.ds_helper.domain.notification.enums.PushTokenDeviceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterPushTokenRequestDto(
        @NotNull
        PushPlatform platform,

        @NotNull
        PushTokenDeviceType deviceType,

        @NotBlank
        String token
) {
}
