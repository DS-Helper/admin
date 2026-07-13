package com.project.ds_helper.domain.volunteer.application.entity;

import com.project.ds_helper.common.enums.ErrorCode;
import com.project.ds_helper.common.exception.BusinessException;
import com.project.ds_helper.domain.base.entity.BaseTime;
import com.project.ds_helper.domain.user.entity.User;
import com.project.ds_helper.domain.volunteer.common.enums.VolunteerApplicationStatus;
import com.project.ds_helper.domain.volunteer.common.persistence.VolunteerInstantConverter;
import com.project.ds_helper.domain.volunteer.file.entity.VolunteerFile;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "tb_volunteer_application")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class VolunteerApplication extends BaseTime {

    @Id
    @Column(name = "volunteer_application_id", length = 36)
    private String id; // 봉사단 가입 신청 식별자

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 신청 사용자

    @Column(name = "name", nullable = false, length = 50)
    private String name; // 신청자 이름

    @Column(name = "phone", nullable = false, length = 20)
    private String phone; // 신청자 연락처

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate; // 생년월일

    @Column(name = "gender", nullable = false, length = 20)
    private String gender; // 성별

    @Column(name = "neighborhood", nullable = false, length = 100)
    private String neighborhood; // 거주 동네

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "tb_volunteer_application_activity",
            joinColumns = @JoinColumn(name = "volunteer_application_id")
    )
    @Column(name = "activity", nullable = false, length = 100)
    @Builder.Default
    private Set<String> preferredActivities = new LinkedHashSet<>(); // 희망 봉사활동 목록

    @Column(name = "motivation", nullable = false, columnDefinition = "TEXT")
    private String motivation; // 지원동기

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "photo_file_id", nullable = false)
    private VolunteerFile photoFile; // 관리자 확인용 비공개 본인 사진

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VolunteerApplicationStatus status; // 가입 신청 상태

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason; // 사용자에게 공개하는 반려 사유

    @Column(name = "admin_memo", columnDefinition = "TEXT")
    private String adminMemo; // 관리자 전용 메모

    @Column(name = "reviewed_by", length = 36)
    private String reviewedBy; // 승인 또는 반려 처리 관리자 식별자

    @Column(name = "reviewed_at")
    @Convert(converter = VolunteerInstantConverter.class)
    private Instant reviewedAt; // 승인 또는 반려 처리 시각

    @Version
    @Column(name = "version", nullable = false)
    private Long version; // 낙관적 락 버전

    @PrePersist
    private void assignIdAndDefaults() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (status == null) {
            status = VolunteerApplicationStatus.PENDING;
        }
    }

    public void updatePending(
            String name,
            String phone,
            LocalDate birthDate,
            String gender,
            String neighborhood,
            Set<String> preferredActivities,
            String motivation,
            VolunteerFile replacementPhoto
    ) {
        requirePending();
        this.name = name;
        this.phone = phone;
        this.birthDate = birthDate;
        this.gender = gender;
        this.neighborhood = neighborhood;
        this.preferredActivities.clear();
        this.preferredActivities.addAll(preferredActivities);
        this.motivation = motivation;
        if (replacementPhoto != null) {
            this.photoFile = replacementPhoto;
        }
    }

    public void cancel() {
        requirePending();
        status = VolunteerApplicationStatus.CANCELED;
    }

    public void approve(String adminId, Instant now) {
        requirePending();
        status = VolunteerApplicationStatus.APPROVED;
        reviewedBy = adminId;
        reviewedAt = now;
    }

    public void reject(String adminId, String reason, String memo, Instant now) {
        requirePending();
        status = VolunteerApplicationStatus.REJECTED;
        rejectionReason = reason;
        adminMemo = memo;
        reviewedBy = adminId;
        reviewedAt = now;
    }

    private void requirePending() {
        if (status != VolunteerApplicationStatus.PENDING) {
            throw new BusinessException(ErrorCode.CONFLICT, "대기 상태 신청서만 변경할 수 있습니다.");
        }
    }
}
