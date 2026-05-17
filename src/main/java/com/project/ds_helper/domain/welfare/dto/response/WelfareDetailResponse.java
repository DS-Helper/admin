package com.project.ds_helper.domain.welfare.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
  * 복지 혜택 상세 정보 응답 DTO
  */
@Schema(description = "복지 혜택 상세 정보 응답 DTO")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WelfareDetailResponse {

    @Schema(description = "혜택명", example = "아동수당 지급")
    private String serviceName; // 혜택명

    @Schema(description = "지원대상 상세", example = "만 8세 미만의 아동을 양육하는 부모")
    private String targetDetailContent; // 지원대상 상세

    @Schema(description = "지원 내용", example = "매월 10만원 지급")
    private String benefitContent; // 지원 내용

    @Schema(description = "선정기준", example = "소득 수준에 관계없이 지급")
    private String selectionCriteriaContent; // 선정기준

    @Schema(description = "제공방식", example = "현금")
    private String serviceProvisionName; // 제공방식

    @Schema(description = "신청 방법", example = "복지로 홈페이지 또는 주민센터 방문")
    private String applicationMethodList; // 신청 방법

    @Schema(description = "문의처", example = "보건복지상담센터 129")
    private String representativeContact; // 문의처

    @Schema(description = "원문 링크", example = "https://www.bokjiro.go.kr")
    private String homepageUrl; // 원문 링크

    @Schema(description = "문의처 목록 원문 JSON")
    private String inquiryContactList;

    @Schema(description = "홈페이지 목록 원문 JSON")
    private String homepageList;

    @Schema(description = "근거 법령 목록 원문 JSON")
    private String basisLawList;

    @Schema(description = "신청 서식 목록 원문 JSON")
    private String basisFormList;

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
                .inquiryContactList(entity.getInquiryContactList())
                .homepageList(entity.getHomepageList())
                .basisLawList(entity.getBasisLawList())
                .basisFormList(entity.getBasisFormList())
                .build();
    }
}
