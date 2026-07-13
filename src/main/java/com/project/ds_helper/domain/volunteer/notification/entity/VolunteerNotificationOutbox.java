package com.project.ds_helper.domain.volunteer.notification.entity;

import com.project.ds_helper.domain.base.entity.BaseTime;
import com.project.ds_helper.domain.volunteer.common.enums.VolunteerOutboxEventType;
import com.project.ds_helper.domain.volunteer.common.enums.VolunteerOutboxStatus;
import com.project.ds_helper.domain.volunteer.common.persistence.VolunteerInstantConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.Duration;
import java.util.UUID;

@Entity
@Table(name = "tb_volunteer_notification_outbox")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class VolunteerNotificationOutbox extends BaseTime {

    @Id
    @Column(name = "volunteer_outbox_id", length = 36)
    private String id; // 알림 Outbox 식별자

    @Column(name = "event_key", nullable = false, unique = true, length = 255)
    private String eventKey; // 중복 알림 방지 키

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private VolunteerOutboxEventType eventType; // 알림 발생 유형

    @Column(name = "receiver_id", nullable = false, length = 36)
    private String receiverId; // 알림 수신 사용자 식별자

    @Column(name = "aggregate_id", nullable = false, length = 36)
    private String aggregateId; // 알림 대상 도메인 식별자

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload; // 알림 발송용 JSON 데이터

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VolunteerOutboxStatus status; // 발송 처리 상태

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount; // 발송 재시도 횟수

    @Column(name = "next_retry_at")
    @Convert(converter = VolunteerInstantConverter.class)
    private Instant nextRetryAt; // 다음 발송 재시도 시각

    @Column(name = "sent_at")
    @Convert(converter = VolunteerInstantConverter.class)
    private Instant sentAt; // 발송 완료 시각

    @PrePersist
    private void assignIdAndDefaults() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (status == null) {
            status = VolunteerOutboxStatus.PENDING;
        }
        if (retryCount == null) {
            retryCount = 0;
        }
    }

    public void markSent(Instant now) {
        status = VolunteerOutboxStatus.SENT;
        sentAt = now;
        nextRetryAt = null;
    }

    public void markFailed(Instant now) {
        status = VolunteerOutboxStatus.FAILED;
        retryCount = retryCount + 1;
        long delayMinutes = Math.min(60L, 1L << Math.min(retryCount - 1, 6));
        nextRetryAt = now.plus(Duration.ofMinutes(delayMinutes));
    }
}
