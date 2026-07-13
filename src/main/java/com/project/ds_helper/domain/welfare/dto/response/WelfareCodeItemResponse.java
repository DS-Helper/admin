package com.project.ds_helper.domain.welfare.dto.response;

import com.project.ds_helper.domain.welfare.entity.WelfareCodeEntity;
import com.project.ds_helper.domain.welfare.entity.WelfareCodeGroup;
import lombok.Builder;

@Builder
public record WelfareCodeItemResponse(
        String codeId,
        WelfareCodeGroup codeGroup,
        String codeValue,
        String codeNameKo,
        String normalizedName,
        Integer sortOrder,
        Boolean active
) {
    public static WelfareCodeItemResponse from(WelfareCodeEntity entity) {
        return WelfareCodeItemResponse.builder()
                .codeId(entity.getCodeId())
                .codeGroup(entity.getCodeGroup())
                .codeValue(entity.getCodeValue())
                .codeNameKo(entity.getCodeNameKo())
                .normalizedName(entity.getNormalizedName())
                .sortOrder(entity.getSortOrder())
                .active(entity.getActive())
                .build();
    }
}
