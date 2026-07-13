package com.project.ds_helper.domain.notification.service;

import com.project.ds_helper.common.dto.response.CursorResponseDto;
import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.notification.dto.response.GetNotificationsResponseDto;
import com.project.ds_helper.domain.notification.dto.response.UnreadNotificationCountResponseDto;
import com.project.ds_helper.domain.notification.entity.Notification;
import com.project.ds_helper.domain.notification.repository.NotificationRepository;
import com.project.ds_helper.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationQueryServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserUtil userUtil;

    @Mock
    private Authentication authentication;

    @Test
    @DisplayName("내 알림 조회는 커서 기반 응답을 반환한다")
    void getMyNotifications_returnsCursorResponse() {
        Notification first = notification("n1", "u1", "첫 번째", LocalDateTime.of(2026, 4, 3, 11, 0, 0));
        Notification second = notification("n2", "u1", "두 번째", LocalDateTime.of(2026, 4, 3, 10, 0, 0));
        NotificationQueryService service = new NotificationQueryService(notificationRepository, userUtil);
        when(userUtil.extractUserId(authentication)).thenReturn("u1");
        when(notificationRepository.findNotificationsWithCursor(anyString(), isNull(), isNull(), any()))
                .thenReturn(new java.util.ArrayList<>(List.of(first, second)));

        CursorResponseDto<GetNotificationsResponseDto> result = service.getMyNotifications(authentication, null, null, 1);

        assertThat(result.content()).hasSize(1);
        assertThat(result.hasNext()).isTrue();
        assertThat(result.cursorId()).isEqualTo("n1");
    }

    @Test
    @DisplayName("커서는 시간과 ID가 함께 와야 한다")
    void getMyNotifications_throwsWhenCursorMismatch() {
        NotificationQueryService service = new NotificationQueryService(notificationRepository, userUtil);

        assertThatThrownBy(() -> service.getMyNotifications(authentication, LocalDateTime.now(), null, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cursorTime and cursorId must be provided together");
    }

    @Test
    @DisplayName("size는 양수여야 한다")
    void getMyNotifications_throwsWhenSizeInvalid() {
        NotificationQueryService service = new NotificationQueryService(notificationRepository, userUtil);

        assertThatThrownBy(() -> service.getMyNotifications(authentication, null, null, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("size must be positive");
    }

    @Test
    @DisplayName("미읽음 알림 개수를 반환한다")
    void getUnreadNotificationCount_returnsCount() {
        NotificationQueryService service = new NotificationQueryService(notificationRepository, userUtil);
        when(userUtil.extractUserId(authentication)).thenReturn("u1");
        when(notificationRepository.countByUser_IdAndIsReadFalse("u1")).thenReturn(2L);

        UnreadNotificationCountResponseDto result = service.getUnreadNotificationCount(authentication);

        assertThat(result.unreadCount()).isEqualTo(2L);
    }

    private Notification notification(String id, String userId, String content, LocalDateTime createdAt) {
        Notification notification = Notification.builder()
                .id(id)
                .user(User.builder().id(userId).build())
                .content(content)
                .build();
        ReflectionTestUtils.setField(notification, "createdAt", createdAt);
        return notification;
    }
}
