package com.project.ds_helper.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record WithdrawUserResponseDto(
        @Schema(description = "삭제된 사용자 ID", example = "user-uuid")
        String userId,

        @Schema(description = "계정 삭제일시", example = "2026-05-21T14:30:00")
        LocalDateTime deletedAt
) {
}
