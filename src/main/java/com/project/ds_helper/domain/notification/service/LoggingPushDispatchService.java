package com.project.ds_helper.domain.notification.service;

import com.project.ds_helper.domain.notification.entity.Notification;
import com.project.ds_helper.domain.notification.entity.NotificationDelivery;
import com.project.ds_helper.domain.notification.entity.PushToken;
import com.project.ds_helper.domain.notification.enums.NotificationDeliveryStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class LoggingPushDispatchService implements PushDispatchService {

    @Override
    public List<NotificationDelivery> dispatch(Notification notification, List<PushToken> pushTokens) {
        // 1. 현재 저장소에는 실제 Push Provider 키 설정이 없으므로 발송 시도 이력만 생성한다.
        log.debug("LoggingPushDispatchService.dispatch started. notificationId={}, pushTokenCount={}", notification.getId(), pushTokens == null ? 0 : pushTokens.size());

        if (pushTokens == null || pushTokens.isEmpty()) {
            log.debug("LoggingPushDispatchService.dispatch skipped. no active push tokens");
            return List.of();
        }

        // 2. 각 토큰별로 스킵 이력을 남겨 이후 실제 FCM/APNs 연동 시 교체 가능한 구조를 유지한다.
        List<NotificationDelivery> deliveries = new ArrayList<>();
        for (PushToken pushToken : pushTokens) {
            log.debug(
                    "LoggingPushDispatchService.dispatch building skipped delivery. notificationId={}, pushTokenId={}, platform={}, deviceType={}",
                    notification.getId(),
                    pushToken.getId(),
                    pushToken.getPlatform(),
                    pushToken.getDeviceType()
            );

            deliveries.add(NotificationDelivery.builder()
                    .notification(notification)
                    .pushToken(pushToken)
                    .status(NotificationDeliveryStatus.SKIPPED)
                    .failureReason("Push provider integration not configured")
                    .sentAt(LocalDateTime.now())
                    .build());
        }

        log.debug("LoggingPushDispatchService.dispatch completed. deliveryCount={}", deliveries.size());
        return deliveries;
    }
}
