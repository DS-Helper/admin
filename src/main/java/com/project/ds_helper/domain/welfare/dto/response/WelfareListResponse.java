package com.project.ds_helper.domain.welfare.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 복지 혜택 목록 응답 DTO (추천 결과용)
 */
@Schema(description = "복지 혜택 목록 응답 DTO (추천 결과용)")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WelfareListResponse {

    @Schema(description = "서비스 ID", example = "WLF00000001")
    private String serviceId; // 서비스 ID

    @Schema(description = "혜택명", example = "아동수당 지급")
    private String serviceName; // 혜택명

    @Schema(description = "혜택 한 줄 요약", example = "아동의 건강한 성장을 위해 매월 수당을 지급합니다.")
    private String serviceSummary; // 혜택 한 줄 요약

    @Schema(description = "지원대상", example = "만 8세 미만의 아동")
    private String targetAudienceArray; // 지원대상 (분류 명칭 등으로 변환 가능)

    @Schema(description = "지원대상 코드 목록")
    private List<WelfareCodeRefResponse> targetAudienceCodes;

    @Schema(description = "제공방식", example = "현금")
    private String serviceProvisionName; // 제공방식

    @Schema(description = "문의처", example = "보건복지상담센터 129")
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
                .targetAudienceCodes(null)
                .serviceProvisionName(entity.getServiceProvisionName())
                .representativeContact(entity.getRepresentativeContact())
                .build();
    }
}
