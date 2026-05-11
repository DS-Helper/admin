package com.project.ds_helper.domain.welfare.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 복지 혜택 추천 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WelfareRecommendRequest {

    @NotBlank(message = "시도명은 필수 입력값입니다.")
    private String cityProvinceName; // 거주 지역 (시도명)

    @NotBlank(message = "시군구명은 필수 입력값입니다.")
    private String districtName; // 거주 지역 (시군구명)

    @NotNull(message = "나이는 필수 입력값입니다.")
    private Integer age; // 사용자 나이

    @NotBlank(message = "필요한 도움 유형은 필수 입력값입니다.")
    private String interestTheme; // 필요한 도움 유형 (코드)

    private String targetCondition; // 해당 조건 (코드, 선택사항)
}
