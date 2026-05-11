package com.project.ds_helper.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;

public record OauthWithdrawRequestDto(
        @NotBlank
        String accessToken
) {
}
