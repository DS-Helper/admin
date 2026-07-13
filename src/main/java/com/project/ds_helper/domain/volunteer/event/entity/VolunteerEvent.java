package com.project.ds_helper.domain.volunteer.event.entity;

import com.project.ds_helper.domain.base.entity.BaseTime;
import com.project.ds_helper.domain.volunteer.common.enums.VolunteerEventCloseReason;
import com.project.ds_helper.domain.volunteer.common.enums.VolunteerEventStatus;
import com.project.ds_helper.domain.volunteer.common.enums.VolunteerEventVisibility;
import com.project.ds_helper.domain.volunteer.common.persistence.VolunteerInstantConverter;
import com.project.ds_helper.domain.volunteer.file.entity.VolunteerFile;
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
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tb_volunteer_event")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class VolunteerEvent extends BaseTime {

    @Id
    @Column(name = "volunteer_event_id", length = 36)
    private String id; // 봉사 일정 식별자

    @Column(name = "title", nullable = false, length = 255)
    private String title; // 봉사 제목

    @Column(name = "type", nullable = false, length = 100)
    private String type; // 봉사 유형

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "image_file_id", nullable = false)
    private VolunteerFile imageFile; // 일정 대표 이미지

    @Column(name = "start_at", nullable = false)
    @Convert(converter = VolunteerInstantConverter.class)
    private Instant startAt; // 봉사 시작 시각(UTC)

    @Column(name = "end_at", nullable = false)
    @Convert(converter = VolunteerInstantConverter.class)
    private Instant endAt; // 봉사 종료 시각(UTC)

    @Column(name = "recruitment_deadline_at", nullable = false)
    @Convert(converter = VolunteerInstantConverter.class)
    private Instant recruitmentDeadlineAt; // 모집 마감 시각(UTC)

    @Column(name = "location", nullable = false, length = 255)
    private String location; // 봉사 장소

    @Column(name = "capacity", nullable = false)
    private Integer capacity; // 모집 인원

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description; // 활동 내용

    @Column(name = "supplies", columnDefinition = "TEXT")
    private String supplies; // 준비물

    @Column(name = "precautions", columnDefinition = "TEXT")
    private String precautions; // 유의사항

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private VolunteerEventStatus status; // 일정 상태

    @Enumerated(EnumType.STRING)
    @Column(name = "close_reason", length = 20)
    private VolunteerEventCloseReason closeReason; // 모집 마감 사유

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 20)
    private VolunteerEventVisibility visibility; // 공개 여부

    @Column(name = "cancel_reason", length = 1000)
    private String cancelReason; // 일정 취소 사유

    @Column(name = "created_by", nullable = false, length = 36)
    private String createdBy; // 일정 등록 관리자 식별자

    @Column(name = "updated_by", nullable = false, length = 36)
    private String updatedBy; // 일정 최종 수정 관리자 식별자

    @Version
    @Column(name = "version", nullable = false)
    private Long version; // 낙관적 락 버전

    @PrePersist
    private void assignId() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }

    public void closeByCapacity() {
        status = VolunteerEventStatus.CLOSED;
        closeReason = VolunteerEventCloseReason.CAPACITY;
    }

    public void open() {
        if (status != VolunteerEventStatus.DRAFT && status != VolunteerEventStatus.CLOSED) {
            throw new IllegalStateException("작성중 또는 마감 일정만 모집을 열 수 있습니다.");
        }
        status = VolunteerEventStatus.OPEN;
        closeReason = null;
    }

    public void closeManually() {
        if (status != VolunteerEventStatus.OPEN) {
            throw new IllegalStateException("모집중 일정만 마감할 수 있습니다.");
        }
        status = VolunteerEventStatus.CLOSED;
        closeReason = VolunteerEventCloseReason.MANUAL;
    }

    public void cancel(String reason) {
        if (status != VolunteerEventStatus.OPEN && status != VolunteerEventStatus.CLOSED) {
            throw new IllegalStateException("모집중 또는 마감 일정만 취소할 수 있습니다.");
        }
        status = VolunteerEventStatus.CANCELED;
        cancelReason = reason;
    }

    public void update(
            String title,
            String type,
            VolunteerFile imageFile,
            Instant startAt,
            Instant endAt,
            Instant recruitmentDeadlineAt,
            String location,
            Integer capacity,
            String description,
            String supplies,
            String precautions,
            VolunteerEventVisibility visibility,
            String adminId
    ) {
        if (status == VolunteerEventStatus.COMPLETED || status == VolunteerEventStatus.CANCELED) {
            throw new IllegalStateException("완료 또는 취소 일정은 수정할 수 없습니다.");
        }
        this.title = title;
        this.type = type;
        this.imageFile = imageFile;
        this.startAt = startAt;
        this.endAt = endAt;
        this.recruitmentDeadlineAt = recruitmentDeadlineAt;
        this.location = location;
        this.capacity = capacity;
        this.description = description;
        this.supplies = supplies;
        this.precautions = precautions;
        this.visibility = visibility;
        this.updatedBy = adminId;
    }

    public void reopenAfterCancellation() {
        if (status == VolunteerEventStatus.CLOSED && closeReason == VolunteerEventCloseReason.CAPACITY) {
            status = VolunteerEventStatus.OPEN;
            closeReason = null;
        }
    }
}
