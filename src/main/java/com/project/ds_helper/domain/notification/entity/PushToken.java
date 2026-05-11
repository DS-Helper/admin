package com.project.ds_helper.domain.notification.entity;

import com.project.ds_helper.domain.base.entity.BaseTime;
import com.project.ds_helper.domain.notification.enums.PushPlatform;
import com.project.ds_helper.domain.notification.enums.PushTokenDeviceType;
import com.project.ds_helper.domain.user.entity.User;
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
        name = "tb_push_token",
        indexes = {
                @Index(name = "idx_push_token_user_active", columnList = "user_id, is_active"),
                @Index(name = "idx_push_token_token", columnList = "token")
        }
)
public class PushToken extends BaseTime {

    @PrePersist
    private void prePersistGenerateId() {
        this.id = String.valueOf(UUID.randomUUID());
    }

    @Id
    @Column(name = "push_token_id")
    private String id; // 푸시 토큰 자체를 식별하는 고유 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 해당 푸시 토큰을 소유한 사용자

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false)
    private PushPlatform platform; // 토큰이 사용되는 플랫폼 구분(WEB, ANDROID, IOS)

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false)
    private PushTokenDeviceType deviceType; // 플랫폼 내부의 세부 디바이스 또는 브라우저 유형

    @Column(name = "token", nullable = false, unique = true, length = 512)
    private String token; // 외부 Push Provider가 사용하는 실제 발송 토큰 값

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true; // 현재 이 토큰으로 발송 가능한 활성 상태 여부

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt; // 마지막 등록 또는 갱신이 확인된 시점

    public void refresh(PushPlatform platform, PushTokenDeviceType deviceType, String token, LocalDateTime lastSeenAt) {
        this.platform = platform;
        this.deviceType = deviceType;
        this.token = token;
        this.isActive = true;
        this.lastSeenAt = lastSeenAt;
    }

    public void deactivate(LocalDateTime lastSeenAt) {
        this.isActive = false;
        this.lastSeenAt = lastSeenAt;
    }
}
