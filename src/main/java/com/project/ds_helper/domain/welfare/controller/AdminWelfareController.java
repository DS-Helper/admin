package com.project.ds_helper.domain.welfare.controller;

import com.project.ds_helper.common.enums.SwaggerTagName;
import com.project.ds_helper.domain.welfare.dto.response.WelfareSyncResultResponse;
import com.project.ds_helper.domain.welfare.service.WelfareSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/welfare")
@Tag(name = SwaggerTagName.WELFARE, description = "복지 서비스 관리자 API")
public class AdminWelfareController {

    private final WelfareSyncService welfareSyncService;

    @Operation(summary = "복지 데이터 수동 동기화", description = "관리자가 공공데이터포털 목록/상세 복지 데이터를 즉시 동기화합니다.")
    @PostMapping("/sync")
    public ResponseEntity<WelfareSyncResultResponse> syncWelfareData() {
        log.debug("AdminWelfareController.syncWelfareData called.");
        return ResponseEntity.ok(welfareSyncService.syncAllWelfareData());
    }

    @Operation(summary = "복지 상세 동기화 실패 재시도", description = "상세 API 동기화에 실패한 활성 복지 서비스만 다시 동기화합니다.")
    @PostMapping("/retry-failed-details")
    public ResponseEntity<WelfareSyncResultResponse> retryFailedDetailSync() {
        log.debug("AdminWelfareController.retryFailedDetailSync called.");
        return ResponseEntity.ok(welfareSyncService.retryFailedDetailSync());
    }
}
