package com.project.ds_helper.domain.welfare.entity;

import com.project.ds_helper.domain.base.entity.BaseTime;
import jakarta.persistence.*;
import lombok.*;

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

    /**
     * 상세 정보를 업데이트하는 메서드
     */
    public void updateDetailInfo(String targetDetailContent, String benefitContent, String selectionCriteriaContent, String applicationMethodList, String homepageUrl) {
        this.targetDetailContent = targetDetailContent;
        this.benefitContent = benefitContent;
        this.selectionCriteriaContent = selectionCriteriaContent;
        this.applicationMethodList = applicationMethodList;
        this.homepageUrl = homepageUrl;
    }
}
