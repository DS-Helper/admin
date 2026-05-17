package com.project.ds_helper.domain.welfare.dto.external;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 공공데이터포털 복지서비스 API의 개별 아이템 DTO
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WelfareApiItem {

    @JsonProperty("servId")
    private String serviceId; // 서비스 ID

    @JsonProperty("servNm")
    private String serviceName; // 서비스 명칭

    @JsonProperty("jurMnstNm")
    @JsonAlias("bizChrDeptNm")
    private String jurisdictionMinistryName; // 소관부처명

    @JsonProperty("jurOrgNm")
    private String jurisdictionOrganizationName; // 소관조직명

    @JsonProperty("ctpvNm")
    private String cityProvinceName; // 시도명

    @JsonProperty("sggNm")
    private String districtName; // 시군구명

    @JsonProperty("servDgst")
    private String serviceSummary; // 서비스 요약

    @JsonProperty("servDetlUrl")
    @JsonAlias("servDtlLink")
    private String serviceDetailUrl; // 서비스 상세 URL

    @JsonProperty("trgterIndvdlArray")
    @JsonAlias("trgterIndvdlNmArray")
    private String targetIndividualArray; // 지원대상 분류

    @JsonProperty("lifeArray")
    @JsonAlias("lifeNmArray")
    private String lifeCycleArray; // 생애주기 분류

    @JsonProperty("intrsThemaArray")
    @JsonAlias("intrsThemaNmArray")
    private String interestThemeArray; // 관심주제 분류

    @JsonProperty("srvPvsnNm")
    private String serviceProvisionName; // 제공방식

    @JsonProperty("rprsCtadr")
    @JsonAlias("inqNum")
    private String representativeContact; // 문의처

    @JsonProperty("lastModYmd")
    private String lastModifiedDate; // 마지막 수정일
}
