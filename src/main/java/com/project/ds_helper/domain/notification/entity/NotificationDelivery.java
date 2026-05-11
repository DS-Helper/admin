package com.project.ds_helper.domain.notification.entity;

import com.project.ds_helper.domain.base.entity.BaseTime;
import com.project.ds_helper.domain.notification.enums.NotificationDeliveryStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "tb_notification_delivery",
        indexes = {
                @Index(name = "idx_notification_delivery_notification", columnList = "notification_id"),
                @Index(name = "idx_notification_delivery_push_token", columnList = "push_token_id"),
                @Index(name = "idx_notification_delivery_status", columnList = "status")
        }
)
public class NotificationDelivery extends BaseTime {

    @PrePersist
    private void prePersistGenerateId() {
        this.id = String.valueOf(UUID.randomUUID());
    }

    @Id
    @Column(name = "delivery_id")
    private String id; // 발송 이력 자체를 식별하는 고유 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification; // 어떤 인앱 알림에 대한 발송 이력인지 연결하는 참조

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "push_token_id", nullable = false)
    private PushToken pushToken; // 어떤 푸시 토큰으로 발송을 시도했는지 나타내는 참조

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private NotificationDeliveryStatus status; // 발송 시도의 최종 상태

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason; // 발송 실패 또는 스킵 사유

    @Column(name = "sent_at")
    private LocalDateTime sentAt; // 실제 발송 또는 발송 시도 시각
}
