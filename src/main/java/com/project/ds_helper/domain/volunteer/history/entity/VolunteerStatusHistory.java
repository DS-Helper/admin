package com.project.ds_helper.domain.volunteer.history.entity;

import com.project.ds_helper.domain.base.entity.BaseTime;
import com.project.ds_helper.domain.volunteer.common.enums.VolunteerHistoryTargetType;
import jakarta.persistence.Column;
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

import java.util.UUID;

@Entity
@Table(name = "tb_volunteer_status_history")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class VolunteerStatusHistory extends BaseTime {

    @Id
    @Column(name = "volunteer_status_history_id", length = 36)
    private String id; // 상태 변경 이력 식별자

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    private VolunteerHistoryTargetType targetType; // 변경 대상 유형

    @Column(name = "target_id", nullable = false, length = 36)
    private String targetId; // 변경 대상 식별자

    @Column(name = "previous_status", length = 30)
    private String previousStatus; // 변경 전 상태

    @Column(name = "next_status", nullable = false, length = 30)
    private String nextStatus; // 변경 후 상태

    @Column(name = "changed_by", nullable = false, length = 36)
    private String changedBy; // 상태 변경 사용자 또는 관리자 식별자

    @Column(name = "change_reason", length = 1000)
    private String changeReason; // 상태 변경 사유

    @PrePersist
    private void assignId() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }
}
