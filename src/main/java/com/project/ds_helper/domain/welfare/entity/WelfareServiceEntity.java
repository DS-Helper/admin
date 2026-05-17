package com.project.ds_helper.domain.welfare.entity;

import com.project.ds_helper.domain.base.entity.BaseTime;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 복지 혜택 정보 엔티티
 * 공공데이터 API(창원시 생애주기별 복지서비스 등)에서 수집한 원천 데이터를 저장합니다.
 */
@Entity
@Table(name = "tb_welfare_service")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
@Builder
public class WelfareServiceEntity extends BaseTime {

    @Id
    @Column(name = "service_id", length = 50)
    private String serviceId; // 서비스 식별자 (공공데이터 API의 servId 사용)

    @Column(name = "service_name", nullable = false, length = 255)
    private String serviceName; // 혜택명 (servNm)

    @Column(name = "city_province_name", length = 50)
    private String cityProvinceName; // 수집 요청 기준 시도명(ctpvNm)

    @Column(name = "district_name", length = 50)
    private String districtName; // 수집 요청 기준 시군구명(sggNm)

    @Column(name = "service_summary", length = 1000)
    private String serviceSummary; // 혜택 한 줄 요약 (servDgst)

    @Column(name = "jurisdiction_ministry_name", length = 100)
    private String jurisdictionMinistryName; // 소관부처/기관명 (jurMnstNm)

    @Column(name = "target_audience_array", length = 500)
    private String targetAudienceArray; // 지원대상 분류 코드들 (trgterIndvdlArray)

    @Column(name = "life_cycle_array", length = 255)
    private String lifeCycleArray; // 생애주기 분류 코드들 (lifeArray)

    @Column(name = "interest_theme_array", length = 255)
    private String interestThemeArray; // 관심주제(필요한 도움 유형) 분류 코드들 (intrsThemaArray)

    @Column(name = "service_provision_name", length = 100)
    private String serviceProvisionName; // 제공방식 (srvPvsnNm)

    @Column(name = "representative_contact", length = 100)
    private String representativeContact; // 문의처 (rprsCtadr)

    @Column(name = "last_modified_date", length = 8)
    private String lastModifiedDate; // 공공데이터포털 목록 응답의 마지막 수정일(lastModYmd)

    @Column(name = "target_detail_content", columnDefinition = "TEXT")
    private String targetDetailContent; // 상세 지원대상 (상세 API: tgtrDtlCn)

    @Column(name = "benefit_content", columnDefinition = "TEXT")
    private String benefitContent; // 지원 내용 (상세 API: alwServCn)

    @Column(name = "selection_criteria_content", columnDefinition = "TEXT")
    private String selectionCriteriaContent; // 선정기준 (상세 API: slctCritCn)

    @Column(name = "application_method_list", columnDefinition = "TEXT")
    private String applicationMethodList; // 신청 방법 (상세 API: applmetList)

    @Column(name = "service_detail_url", length = 500)
    private String serviceDetailUrl; // 서비스 상세 URL (servDetlUrl)

    @Column(name = "homepage_url", length = 500)
    private String homepageUrl; // 원문 링크 (상세 API: 인구보건협회 등 외부 링크)

    @Column(name = "inquiry_contact_list", columnDefinition = "TEXT")
    private String inquiryContactList; // 문의처 목록 원문 JSON(inqplCtadrList)

    @Column(name = "homepage_list", columnDefinition = "TEXT")
    private String homepageList; // 홈페이지 목록 원문 JSON(inqplHmpgReldList)

    @Column(name = "basis_law_list", columnDefinition = "TEXT")
    private String basisLawList; // 근거 법령 목록 원문 JSON(baslawList)

    @Column(name = "basis_form_list", columnDefinition = "TEXT")
    private String basisFormList; // 신청 서식 목록 원문 JSON(basfrmList)

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean active = true; // 공공데이터포털 목록에 현재 존재하는 서비스인지 여부

    @Column(name = "first_seen_at")
    private LocalDateTime firstSeenAt; // 이 서비스가 최초로 수집된 시각

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt; // 마지막 목록 동기화에서 확인된 시각

    @Column(name = "discontinued_at")
    private LocalDateTime discontinuedAt; // 목록에서 사라진 것으로 판단된 시각

    @Column(name = "synced_at")
    private LocalDateTime syncedAt; // 목록 데이터가 마지막으로 동기화된 시각

    @Column(name = "detail_synced_at")
    private LocalDateTime detailSyncedAt; // 상세 데이터가 마지막으로 정상 동기화된 시각

    @Builder.Default
    @Column(name = "detail_sync_failed", nullable = false)
    private Boolean detailSyncFailed = false; // 상세 API 동기화 실패 여부

    @Column(name = "detail_sync_failure_reason", length = 500)
    private String detailSyncFailureReason; // 상세 API 동기화 실패 사유

    /**
     * 목록 API에서 내려오는 기본 정보를 갱신한다.
     * 상세 필드는 별도 상세 API 동기화에서 관리하므로 목록 갱신 시 덮어쓰지 않는다.
     */
    public void updateListInfo(
            String serviceName,
            String cityProvinceName,
            String districtName,
            String serviceSummary,
            String jurisdictionMinistryName,
            String targetAudienceArray,
            String lifeCycleArray,
            String interestThemeArray,
            String serviceProvisionName,
            String representativeContact,
            String serviceDetailUrl,
            String lastModifiedDate
    ) {
        this.serviceName = serviceName;
        this.cityProvinceName = cityProvinceName;
        this.districtName = districtName;
        this.serviceSummary = serviceSummary;
        this.jurisdictionMinistryName = jurisdictionMinistryName;
        this.targetAudienceArray = targetAudienceArray;
        this.lifeCycleArray = lifeCycleArray;
        this.interestThemeArray = interestThemeArray;
        this.serviceProvisionName = serviceProvisionName;
        this.representativeContact = representativeContact;
        this.serviceDetailUrl = serviceDetailUrl;
        this.lastModifiedDate = lastModifiedDate;
    }

    public void markSeen(LocalDateTime seenAt) {
        if (this.firstSeenAt == null) {
            this.firstSeenAt = seenAt;
        }
        this.lastSeenAt = seenAt;
        this.syncedAt = seenAt;
        this.active = true;
        this.discontinuedAt = null;
    }

    /**
     * 상세 정보를 업데이트하는 메서드
     */
    public void updateDetailInfo(String targetDetailContent, String benefitContent, String selectionCriteriaContent, String applicationMethodList, String homepageUrl, LocalDateTime detailSyncedAt) {
        this.targetDetailContent = targetDetailContent;
        this.benefitContent = benefitContent;
        this.selectionCriteriaContent = selectionCriteriaContent;
        this.applicationMethodList = applicationMethodList;
        this.homepageUrl = homepageUrl;
        this.detailSyncedAt = detailSyncedAt;
        this.detailSyncFailed = false;
        this.detailSyncFailureReason = null;
    }

    /**
     * 상세 API의 부가 목록성 데이터를 원문 JSON 형태로 보존한다.
     */
    public void updateDetailRelationInfo(String inquiryContactList, String homepageList, String basisLawList, String basisFormList) {
        this.inquiryContactList = inquiryContactList;
        this.homepageList = homepageList;
        this.basisLawList = basisLawList;
        this.basisFormList = basisFormList;
    }

    public void markDetailSyncFailed(String failureReason) {
        this.detailSyncFailed = true;
        this.detailSyncFailureReason = failureReason == null
                ? null
                : failureReason.substring(0, Math.min(failureReason.length(), 500));
    }

    public void markDiscontinued(LocalDateTime discontinuedAt) {
        this.active = false;
        this.discontinuedAt = discontinuedAt;
    }
}
