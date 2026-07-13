package com.project.ds_helper.domain.notification.service;

import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.notification.entity.Notification;
import com.project.ds_helper.domain.notification.repository.NotificationRepository;
import com.project.ds_helper.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import jakarta.persistence.EntityNotFoundException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationCommandServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private UserUtil userUtil;
    @Mock private Authentication authentication;

    @Test
    @DisplayName("읽음 처리는 본인 알림이면 상태를 바꾼다")
    void markAsRead_marksNotification() {
        NotificationCommandService service = new NotificationCommandService(notificationRepository, userUtil);
        Notification notification = Notification.builder()
                .user(User.builder().id("user-1").build())
                .build();
        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(notificationRepository.findByIdAndUser_Id("n1", "user-1")).thenReturn(Optional.of(notification));

        service.markAsRead(authentication, "n1");

        verify(notificationRepository).findByIdAndUser_Id("n1", "user-1");
    }

    @Test
    @DisplayName("읽음 처리는 알림이 없으면 예외를 던진다")
    void markAsRead_throwsWhenMissing() {
        NotificationCommandService service = new NotificationCommandService(notificationRepository, userUtil);
        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(notificationRepository.findByIdAndUser_Id("n1", "user-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markAsRead(authentication, "n1"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("전체 읽음 처리는 사용자 기준으로 호출한다")
    void markAllAsRead_marksAll() {
        NotificationCommandService service = new NotificationCommandService(notificationRepository, userUtil);
        when(userUtil.extractUserId(authentication)).thenReturn("user-1");

        service.markAllAsRead(authentication);

        verify(notificationRepository).markAllAsReadByUserId("user-1");
    }
}
