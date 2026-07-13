package com.project.ds_helper.domain.welfare.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "복지 코드 그룹")
public enum WelfareCodeGroup {
    @Schema(description = "생애주기 코드 그룹")
    LIFE_ARRAY,
    @Schema(description = "지원대상 코드 그룹")
    TARGET_INDV_ARRAY,
    @Schema(description = "관심주제 코드 그룹")
    INTEREST_THEME_ARRAY,
    @Schema(description = "복지정보 상세 코드 그룹")
    WELFARE_INFO_DETAIL_CODE,
    @Schema(description = "검색 분류 코드 그룹")
    SEARCH_KEY_CODE,
    @Schema(description = "정렬 순서 코드 그룹")
    ARRANGE_ORDER
}
