package com.project.ds_helper.domain.volunteer.member.entity;

import com.project.ds_helper.domain.base.entity.BaseTime;
import com.project.ds_helper.domain.user.entity.User;
import com.project.ds_helper.domain.volunteer.application.entity.VolunteerApplication;
import com.project.ds_helper.domain.volunteer.common.enums.VolunteerMemberStatus;
import com.project.ds_helper.domain.volunteer.common.persistence.VolunteerInstantConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tb_volunteer_member")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class VolunteerMember extends BaseTime {

    @Id
    @Column(name = "volunteer_member_id", length = 36)
    private String id; // 봉사단원 식별자

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user; // 봉사단원 사용자

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "volunteer_application_id", nullable = false, unique = true)
    private VolunteerApplication application; // 승인된 가입 신청서

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VolunteerMemberStatus status; // 봉사단원 활동 상태

    @Column(name = "joined_at", nullable = false)
    @Convert(converter = VolunteerInstantConverter.class)
    private Instant joinedAt; // 봉사단 가입 시각

    @Column(name = "suspended_at")
    @Convert(converter = VolunteerInstantConverter.class)
    private Instant suspendedAt; // 활동정지 시각

    @Column(name = "withdrawn_at")
    @Convert(converter = VolunteerInstantConverter.class)
    private Instant withdrawnAt; // 탈퇴 시각

    @Version
    @Column(name = "version", nullable = false)
    private Long version; // 낙관적 락 버전

    @PrePersist
    private void assignId() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }

    public boolean isActive() {
        return status == VolunteerMemberStatus.ACTIVE;
    }

    public void suspend(Instant now) {
        if (status != VolunteerMemberStatus.ACTIVE) {
            throw new IllegalStateException("활동중 단원만 활동정지할 수 있습니다.");
        }
        status = VolunteerMemberStatus.SUSPENDED;
        suspendedAt = now;
    }

    public void activate() {
        if (status != VolunteerMemberStatus.SUSPENDED) {
            throw new IllegalStateException("활동정지 단원만 활동재개할 수 있습니다.");
        }
        status = VolunteerMemberStatus.ACTIVE;
        suspendedAt = null;
    }

    public void withdraw(Instant now) {
        if (status == VolunteerMemberStatus.WITHDRAWN) {
            throw new IllegalStateException("이미 탈퇴한 단원입니다.");
        }
        status = VolunteerMemberStatus.WITHDRAWN;
        withdrawnAt = now;
    }
}
