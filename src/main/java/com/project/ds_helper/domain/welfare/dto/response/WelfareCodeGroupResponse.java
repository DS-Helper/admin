package com.project.ds_helper.domain.welfare.dto.response;

import com.project.ds_helper.domain.welfare.entity.WelfareCodeGroup;
import lombok.Builder;

import java.util.List;

@Builder
public record WelfareCodeGroupResponse(
        WelfareCodeGroup codeGroup,
        List<WelfareCodeItemResponse> codes
) {
}
