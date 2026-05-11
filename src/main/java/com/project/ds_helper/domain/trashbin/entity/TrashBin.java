package com.project.ds_helper.domain.trashbin.entity;

import com.project.ds_helper.domain.base.entity.BaseTime;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "tb_trash_bin",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_trash_bin_latitude_longitude",
                        columnNames = {"latitude", "longitude"}
                )
        }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrashBin extends BaseTime {

    @PrePersist
    private void prePersistGenerateId() {
        this.id = String.valueOf(UUID.randomUUID());
    }

    @Id
    @Column(name = "trash_bin_id")
    private String id;

    /**
     * CSV의 시도명. 예: 대구광역시.
     */
    @Column(name = "province_name")
    private String provinceName;

    /**
     * CSV의 시군구명. 예: 달성군.
     */
    @Column(name = "city_county_name")
    private String cityCountyName;

    /**
     * 쓰레기통이 설치된 도로명/지번 주소.
     */
    @Column(name = "address")
    private String address;

    /**
     * 프론트 목록 응답에서 바로 사용할 대표 이미지 URL.
     * 실제 이미지 메타데이터는 TrashBinImage와 연관관계로 관리한다.
     */
    @Column(name = "photo_url", columnDefinition = "TEXT")
    private String photoUrl;

    /**
     * 사용자가 현장에서 위치를 찾을 수 있도록 설명하는 텍스트.
     */
    @Column(name = "location_description", columnDefinition = "TEXT")
    private String locationDescription;

    /**
     * CSV의 설치지점 분류. 예: 아파트안, 시설물.
     */
    @Column(name = "installation_point")
    private String installationPoint;

    /**
     * 쓰레기통 종류. 예: 일반쓰레기/재활용.
     */
    @Column(name = "bin_type")
    private String binType;

    /**
     * 해당 쓰레기통 데이터를 관리하는 기관명.
     */
    @Column(name = "management_agency_name")
    private String managementAgencyName;

    /**
     * 관리기관 연락처.
     */
    @Column(name = "management_agency_phone_number")
    private String managementAgencyPhoneNumber;

    /**
     * 이미지 업로드 매핑 기준으로 사용하는 위도.
     */
    @Column(name = "latitude")
    private Double latitude;

    /**
     * 위치 조회와 중복 위도 보정에 사용하는 경도.
     */
    @Column(name = "longitude")
    private Double longitude;

    /**
     * CSV 데이터 기준일자.
     */
    @Column(name = "data_reference_date")
    private LocalDate dataReferenceDate;

    /**
     * 원본 CSV의 존재여부 값. 원본 데이터 추적을 위해 문자열 그대로 보관한다.
     */
    @Column(name = "exists_yn")
    private String existsYn;

    /**
     * S3에 업로드된 쓰레기통 이미지 목록.
     */
    @Builder.Default
    @OneToMany(mappedBy = "trashBin", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TrashBinImage> images = new ArrayList<>();

    public void updatePhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }
}
