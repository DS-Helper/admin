package com.project.ds_helper.domain.notification.entity;

import com.project.ds_helper.domain.base.entity.BaseTime;
import com.project.ds_helper.domain.notification.enums.NotificationType;
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

import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "tb_notification",
        indexes = {
                @Index(name = "idx_notification_user_created", columnList = "user_id, created_at"),
                @Index(name = "idx_notification_user_created_id", columnList = "user_id, created_at, notification_id"),
                @Index(name = "idx_notification_user_is_read", columnList = "user_id, is_read")
        }
)
public class Notification extends BaseTime {

    @PrePersist
    private void prePersistGenerateId() {
        this.id = String.valueOf(UUID.randomUUID());
    }

    @Id
    @Column(name = "notification_id")
    private String id; // 알림 자체를 식별하는 고유 ID

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 알림을 수신하는 사용자

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NotificationType type; // 알림 발생 유형

    @Column(name = "board_id", nullable = false)
    private String boardId; // 알림이 연결된 게시글 ID

    @Column(name = "comment_id", nullable = false)
    private String commentId; // 알림이 연결된 댓글 또는 대댓글 ID

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content; // 사용자에게 보여줄 알림 본문

    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private boolean isRead = false; // 사용자의 알림 읽음 여부

    public void markAsRead() {
        this.isRead = true;
    }
}
