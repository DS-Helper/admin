package com.project.ds_helper.domain.welfare.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "복지 데이터 동기화 결과 응답")
public record WelfareSyncResultResponse(
        @Schema(description = "처리한 전체 목록 건수", example = "4563")
        int totalProcessedCount,

        @Schema(description = "저장 성공 건수", example = "4560")
        int successCount,

        @Schema(description = "저장 실패 건수", example = "3")
        int failureCount,

        @Schema(description = "이번 전체 목록 동기화에서 사라진 것으로 판단되어 비활성 처리한 건수", example = "5")
        int discontinuedCount,

        @Schema(description = "상세 API 동기화 실패 건수", example = "2")
        int detailFailureCount
) {
    public WelfareSyncResultResponse(int totalProcessedCount, int successCount, int failureCount) {
        this(totalProcessedCount, successCount, failureCount, 0, 0);
    }
}
