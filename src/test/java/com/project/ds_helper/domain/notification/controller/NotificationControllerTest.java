package com.project.ds_helper.domain.notification.controller;

import com.project.ds_helper.common.dto.response.CursorResponseDto;
import com.project.ds_helper.common.dto.response.ResponseVo;
import com.project.ds_helper.domain.notification.dto.response.GetNotificationsResponseDto;
import com.project.ds_helper.domain.notification.dto.response.UnreadNotificationCountResponseDto;
import com.project.ds_helper.domain.notification.enums.NotificationType;
import com.project.ds_helper.domain.notification.service.NotificationCommandService;
import com.project.ds_helper.domain.notification.service.NotificationQueryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationQueryService notificationQueryService;

    @Mock
    private NotificationCommandService notificationCommandService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private NotificationController notificationController;

    @Test
    @DisplayName("내 알림 목록 조회는 ResponseVo로 감싼 커서 응답을 반환한다")
    void getMyNotifications_returnsWrappedCursorResponse() {
        GetNotificationsResponseDto notification = new GetNotificationsResponseDto(
                "notification-1",
                NotificationType.BOARD_COMMENT,
                "board-1",
                "comment-1",
                "내 게시글에 댓글이 작성되었습니다.",
                false,
                LocalDateTime.of(2026, 4, 3, 10, 30, 0)
        );

        CursorResponseDto<GetNotificationsResponseDto> responseDto = CursorResponseDto.toDto(
                List.of(notification),
                LocalDateTime.of(2026, 4, 3, 10, 30, 0),
                "notification-1",
                false
        );

        when(notificationQueryService.getMyNotifications(any(), isNull(), isNull(), anyInt()))
                .thenReturn(responseDto);

        ResponseEntity<ResponseVo<CursorResponseDto<GetNotificationsResponseDto>>> response =
                notificationController.getMyNotifications(authentication, null, null, 10);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().content()).hasSize(1);
        assertThat(response.getBody().getData().content().getFirst().notificationId()).isEqualTo("notification-1");
        assertThat(response.getBody().getData().content().getFirst().type()).isEqualTo(NotificationType.BOARD_COMMENT);
    }

    @Test
    @DisplayName("알림 읽음 처리는 성공 응답을 반환한다")
    void markAsRead_returnsSuccessResponse() {
        doNothing().when(notificationCommandService).markAsRead(authentication, "notification-1");

        ResponseEntity<ResponseVo<Void>> response =
                notificationController.markAsRead(authentication, "notification-1");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
    }

    @Test
    @DisplayName("전체 알림 읽음 처리는 성공 응답을 반환한다")
    void markAllAsRead_returnsSuccessResponse() {
        doNothing().when(notificationCommandService).markAllAsRead(authentication);

        ResponseEntity<ResponseVo<Void>> response =
                notificationController.markAllAsRead(authentication);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isTrue();
    }

    @Test
    @DisplayName("미읽음 알림 개수 조회는 ResponseVo로 감싼 응답을 반환한다")
    void getUnreadNotificationCount_returnsWrappedResponse() {
        when(notificationQueryService.getUnreadNotificationCount(authentication))
                .thenReturn(UnreadNotificationCountResponseDto.toDto(5L));

        ResponseEntity<ResponseVo<UnreadNotificationCountResponseDto>> response =
                notificationController.getUnreadNotificationCount(authentication);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().unreadCount()).isEqualTo(5L);
    }
}
