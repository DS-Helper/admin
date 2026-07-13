package com.project.ds_helper.domain.volunteer.participation.entity;

import com.project.ds_helper.common.enums.ErrorCode;
import com.project.ds_helper.common.exception.BusinessException;
import com.project.ds_helper.domain.base.entity.BaseTime;
import com.project.ds_helper.domain.volunteer.common.enums.VolunteerParticipationStatus;
import com.project.ds_helper.domain.volunteer.common.persistence.VolunteerInstantConverter;
import com.project.ds_helper.domain.volunteer.event.entity.VolunteerEvent;
import com.project.ds_helper.domain.volunteer.member.entity.VolunteerMember;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "tb_volunteer_participation",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_volunteer_participation_event_member",
                columnNames = {"volunteer_event_id", "volunteer_member_id"}
        )
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class VolunteerParticipation extends BaseTime {

    @Id
    @Column(name = "volunteer_participation_id", length = 36)
    private String id; // 일정 참여 식별자

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "volunteer_event_id", nullable = false)
    private VolunteerEvent event; // 참여 봉사 일정

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "volunteer_member_id", nullable = false)
    private VolunteerMember member; // 참여 봉사단원

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VolunteerParticipationStatus status; // 참여 상태

    @Column(name = "applied_at", nullable = false)
    @Convert(converter = VolunteerInstantConverter.class)
    private Instant appliedAt; // 참여 신청 시각

    @Column(name = "canceled_at")
    @Convert(converter = VolunteerInstantConverter.class)
    private Instant canceledAt; // 참여 취소 시각

    @Column(name = "cancel_reason", length = 1000)
    private String cancelReason; // 참여 취소 사유

    @Column(name = "attendance_checked_at")
    @Convert(converter = VolunteerInstantConverter.class)
    private Instant attendanceCheckedAt; // 출석 확인 시각

    @Column(name = "attendance_checked_by", length = 36)
    private String attendanceCheckedBy; // 출석 확인 관리자 식별자

    @Version
    @Column(name = "version", nullable = false)
    private Long version; // 낙관적 락 버전

    @PrePersist
    private void assignId() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }

    public void reapply(Instant now) {
        if (status != VolunteerParticipationStatus.CANCELED) {
            throw new BusinessException(ErrorCode.CONFLICT, "취소된 참여만 재신청할 수 있습니다.");
        }
        status = VolunteerParticipationStatus.APPLIED;
        appliedAt = now;
        canceledAt = null;
        cancelReason = null;
    }

    public void cancelByUser(Instant now) {
        if (status != VolunteerParticipationStatus.APPLIED) {
            throw new BusinessException(ErrorCode.CONFLICT, "신청 상태 참여만 취소할 수 있습니다.");
        }
        status = VolunteerParticipationStatus.CANCELED;
        canceledAt = now;
        cancelReason = null;
    }

    public void attend(String adminId, Instant now) {
        requireApplied();
        status = VolunteerParticipationStatus.ATTENDED;
        attendanceCheckedBy = adminId;
        attendanceCheckedAt = now;
    }

    public void markAbsent(String adminId, Instant now) {
        requireApplied();
        status = VolunteerParticipationStatus.ABSENT;
        attendanceCheckedBy = adminId;
        attendanceCheckedAt = now;
    }

    private void requireApplied() {
        if (status != VolunteerParticipationStatus.APPLIED) {
            throw new BusinessException(ErrorCode.CONFLICT, "신청 상태 참여만 출석 처리할 수 있습니다.");
        }
    }
}
