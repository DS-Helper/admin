package com.project.ds_helper.domain.welfare.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record WelfareCodeRefResponse(
        @Schema(description = "코드 그룹", example = "TARGET_INDV_ARRAY")
        String codeGroup,
        @Schema(description = "코드 값", example = "040")
        String codeValue,
        @Schema(description = "코드명", example = "장애인")
        String codeNameKo
) {
}
