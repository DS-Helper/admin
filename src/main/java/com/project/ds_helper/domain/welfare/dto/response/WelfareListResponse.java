package com.project.ds_helper.domain.welfare.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 복지 혜택 목록 응답 DTO (추천 결과용)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WelfareListResponse {

    private String serviceId; // 서비스 ID
    private String serviceName; // 혜택명
    private String serviceSummary; // 혜택 한 줄 요약
    private String targetAudienceArray; // 지원대상 (분류 명칭 등으로 변환 가능)
    private String serviceProvisionName; // 제공방식
    private String representativeContact; // 문의처

    /**
     * Entity를 Response DTO로 변환하는 정적 팩토리 메서드
     */
    public static WelfareListResponse from(com.project.ds_helper.domain.welfare.entity.WelfareServiceEntity entity) {
        return WelfareListResponse.builder()
                .serviceId(entity.getServiceId())
                .serviceName(entity.getServiceName())
                .serviceSummary(entity.getServiceSummary())
                .targetAudienceArray(entity.getTargetAudienceArray())
                .serviceProvisionName(entity.getServiceProvisionName())
                .representativeContact(entity.getRepresentativeContact())
                .build();
    }
}
