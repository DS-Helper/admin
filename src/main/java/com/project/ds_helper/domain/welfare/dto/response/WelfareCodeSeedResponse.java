package com.project.ds_helper.domain.welfare.dto.response;

import lombok.Builder;

@Builder
public record WelfareCodeSeedResponse(
        Integer upsertedCount,
        Integer groupCount
) {
}
