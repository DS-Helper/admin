package com.project.ds_helper.domain.welfare.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 복지 혜택 추천 요청 DTO
 */
@Schema(description = "복지 혜택 추천 요청 DTO")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WelfareRecommendRequest {

    @Schema(description = "거주 지역 (시도명)", example = "서울특별시")
    @NotBlank(message = "시도명은 필수 입력값입니다.")
    private String cityProvinceName; // 거주 지역 (시도명)

    @Schema(description = "거주 지역 (시군구명)", example = "강남구")
    @NotBlank(message = "시군구명은 필수 입력값입니다.")
    private String districtName; // 거주 지역 (시군구명)

    @Schema(description = "사용자 나이", example = "25")
    @NotNull(message = "나이는 필수 입력값입니다.")
    private Integer age; // 사용자 나이

    @Schema(description = "관심 주제 (코드)", example = "010")
    @NotBlank(message = "필요한 도움 유형은 필수 입력값입니다.")
    private String interestTheme; // 필요한 도움 유형 (코드)

    @Schema(description = "해당 조건 (코드, 선택사항)", example = "020")
    private String targetCondition; // 해당 조건 (코드, 선택사항)
}
