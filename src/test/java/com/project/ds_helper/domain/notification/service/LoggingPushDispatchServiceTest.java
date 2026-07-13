package com.project.ds_helper.domain.notification.service;

import com.project.ds_helper.domain.notification.entity.Notification;
import com.project.ds_helper.domain.notification.entity.NotificationDelivery;
import com.project.ds_helper.domain.notification.entity.PushToken;
import com.project.ds_helper.domain.notification.enums.NotificationDeliveryStatus;
import com.project.ds_helper.domain.notification.enums.PushPlatform;
import com.project.ds_helper.domain.notification.enums.PushTokenDeviceType;
import com.project.ds_helper.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LoggingPushDispatchServiceTest {

    private final LoggingPushDispatchService service = new LoggingPushDispatchService();

    @Test
    @DisplayName("푸시 토큰이 없으면 빈 목록을 반환한다")
    void dispatch_returnsEmptyWhenNoTokens() {
        Notification notification = Notification.builder().id("n1").user(User.builder().id("u1").build()).build();

        List<NotificationDelivery> result = service.dispatch(notification, List.of());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("푸시 토큰이 있으면 SKIPPED 이력을 생성한다")
    void dispatch_createsSkippedDeliveries() {
        Notification notification = Notification.builder().id("n1").user(User.builder().id("u1").build()).build();
        PushToken token = PushToken.builder()
                .id("p1")
                .user(User.builder().id("u1").build())
                .platform(PushPlatform.WEB)
                .deviceType(PushTokenDeviceType.CHROME)
                .token("token")
                .build();

        List<NotificationDelivery> result = service.dispatch(notification, List.of(token));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getStatus()).isEqualTo(NotificationDeliveryStatus.SKIPPED);
    }
}
