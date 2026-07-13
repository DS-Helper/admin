package com.project.ds_helper.domain.volunteer.file.entity;

import com.project.ds_helper.domain.base.entity.BaseTime;
import com.project.ds_helper.domain.volunteer.common.enums.VolunteerFileType;
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
@Table(name = "tb_volunteer_file")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class VolunteerFile extends BaseTime {

    @Id
    @Column(name = "volunteer_file_id", length = 36)
    private String id; // 봉사 파일 식별자

    @Column(name = "owner_user_id", length = 36)
    private String ownerUserId; // 파일 소유 사용자 식별자

    @Enumerated(EnumType.STRING)
    @Column(name = "file_type", nullable = false, length = 30)
    private VolunteerFileType fileType; // 신청자 사진 또는 일정 이미지 구분

    @Column(name = "s3_key", nullable = false, unique = true, length = 500)
    private String s3Key; // S3 객체 키

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename; // 원본 파일명

    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType; // 저장 파일 MIME 타입

    @Column(name = "file_size", nullable = false)
    private Long fileSize; // 저장 파일 크기(Byte)

    @Column(name = "width", nullable = false)
    private Integer width; // 저장 이미지 너비

    @Column(name = "height", nullable = false)
    private Integer height; // 저장 이미지 높이

    @Column(name = "is_private", nullable = false)
    private boolean privateFile; // 비공개 파일 여부

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true; // 현재 참조 가능한 파일 여부

    @PrePersist
    private void assignId() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
    }

    public void deactivate() {
        active = false;
    }
}
