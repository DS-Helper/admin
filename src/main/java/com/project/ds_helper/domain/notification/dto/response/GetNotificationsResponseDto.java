package com.project.ds_helper.domain.notification.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.project.ds_helper.domain.notification.entity.Notification;
import com.project.ds_helper.domain.notification.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "알림 조회 응답 DTO")
public record GetNotificationsResponseDto(
        @Schema(description = "알림 ID", example = "3f9c7c8e-4d5a-4b1e-9a2f-7c1d2e8f9a01")
        String notificationId,

        @Schema(description = "알림 타입", example = "BOARD_COMMENT")
        NotificationType type,

        @Schema(description = "이동 대상 게시글 ID", example = "3f9c7c8e-4d5a-4b1e-9a2f-7c1d2e8f9a01")
        String boardId,

        @Schema(description = "이동 대상 댓글 ID", example = "3f9c7c8e-4d5a-4b1e-9a2f-7c1d2e8f9a01")
        String commentId,

        @Schema(description = "알림 내용", example = "내 게시글에 댓글이 작성되었습니다.")
        String content,

        @Schema(description = "읽음 여부", example = "false")
        boolean isRead,

        @Schema(description = "알림 생성 일시", example = "2026-04-03 10:30:00")
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime createdAt
) {
    public static GetNotificationsResponseDto toDto(Notification notification) {
        return new GetNotificationsResponseDto(
                notification.getId(),
                notification.getType(),
                notification.getBoardId(),
                notification.getCommentId(),
                notification.getContent(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
