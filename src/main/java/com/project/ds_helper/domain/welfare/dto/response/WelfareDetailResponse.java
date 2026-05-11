package com.project.ds_helper.domain.welfare.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 복지 혜택 상세 정보 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WelfareDetailResponse {

    private String serviceName; // 혜택명
    private String targetDetailContent; // 지원대상 상세
    private String benefitContent; // 지원 내용
    private String selectionCriteriaContent; // 선정기준
    private String serviceProvisionName; // 제공방식
    private String applicationMethodList; // 신청 방법
    private String representativeContact; // 문의처
    private String homepageUrl; // 원문 링크

    /**
     * Entity를 Detail Response DTO로 변환하는 정적 팩토리 메서드
     */
    public static WelfareDetailResponse from(com.project.ds_helper.domain.welfare.entity.WelfareServiceEntity entity) {
        return WelfareDetailResponse.builder()
                .serviceName(entity.getServiceName())
                .targetDetailContent(entity.getTargetDetailContent())
                .benefitContent(entity.getBenefitContent())
                .selectionCriteriaContent(entity.getSelectionCriteriaContent())
                .serviceProvisionName(entity.getServiceProvisionName())
                .applicationMethodList(entity.getApplicationMethodList())
                .representativeContact(entity.getRepresentativeContact())
                .homepageUrl(entity.getHomepageUrl())
                .build();
    }
}
