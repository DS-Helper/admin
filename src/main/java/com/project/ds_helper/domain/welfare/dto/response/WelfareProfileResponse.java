package com.project.ds_helper.domain.welfare.dto.response;

import com.project.ds_helper.domain.welfare.entity.WelfareProfile;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record WelfareProfileResponse(
        @Schema(description = "프로필 ID", example = "1")
        Long id,
        @Schema(description = "시도명", example = "서울특별시")
        String cityProvinceName,
        @Schema(description = "시군구명", example = "강남구")
        String districtName,
        @Schema(description = "나이", example = "25")
        Integer age,
        @Schema(description = "생애주기 코드", example = "004")
        String lifeCycleCode,
        @Schema(description = "생애주기 코드명", example = "청년")
        String lifeCycleName,
        @Schema(description = "관심주제 코드", example = "010")
        String interestThemeCode,
        @Schema(description = "관심주제 코드명", example = "신체건강")
        String interestThemeName,
        @Schema(description = "지원대상 코드", example = "040")
        String targetConditionCode,
        @Schema(description = "지원대상 코드명", example = "장애인")
        String targetConditionName
) {
    public static WelfareProfileResponse from(WelfareProfile profile) {
        return WelfareProfileResponse.builder()
                .id(profile.getId())
                .cityProvinceName(profile.getCityProvinceName())
                .districtName(profile.getDistrictName())
                .age(profile.getAge())
                .lifeCycleCode(profile.getLifeCycle())
                .lifeCycleName(null)
                .interestThemeCode(profile.getInterestTheme())
                .interestThemeName(null)
                .targetConditionCode(profile.getTargetCondition())
                .targetConditionName(null)
                .build();
    }
}
