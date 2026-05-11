package com.project.ds_helper.domain.notification.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "미읽음 알림 개수 응답 DTO")
public record UnreadNotificationCountResponseDto(
        @Schema(description = "미읽음 알림 개수", example = "3")
        long unreadCount
) {
    public static UnreadNotificationCountResponseDto toDto(long unreadCount) {
        return new UnreadNotificationCountResponseDto(unreadCount);
    }
}
