package com.project.ds_helper.domain.welfare.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 공공데이터포털 복지서비스 API의 개별 아이템 DTO
 */
@Getter
@NoArgsConstructor
public class WelfareApiItem {

    @JsonProperty("servId")
    private String serviceId; // 서비스 ID

    @JsonProperty("servNm")
    private String serviceName; // 서비스 명칭

    @JsonProperty("jurMnstNm")
    private String jurisdictionMinistryName; // 소관부처명

    @JsonProperty("jurOrgNm")
    private String jurisdictionOrganizationName; // 소관조직명

    @JsonProperty("servDgst")
    private String serviceSummary; // 서비스 요약

    @JsonProperty("servDetlUrl")
    private String serviceDetailUrl; // 서비스 상세 URL

    @JsonProperty("trgterIndvdlArray")
    private String targetIndividualArray; // 지원대상 분류

    @JsonProperty("lifeArray")
    private String lifeCycleArray; // 생애주기 분류

    @JsonProperty("intrsThemaArray")
    private String interestThemeArray; // 관심주제 분류

    @JsonProperty("srvPvsnNm")
    private String serviceProvisionName; // 제공방식

    @JsonProperty("rprsCtadr")
    private String representativeContact; // 문의처
}
