package com.project.ds_helper.domain.welfare.controller;

import com.project.ds_helper.domain.welfare.dto.response.WelfareSyncResultResponse;
import com.project.ds_helper.domain.welfare.service.WelfareSyncService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminWelfareControllerTest {

    @Mock
    private WelfareSyncService welfareSyncService;

    @InjectMocks
    private AdminWelfareController adminWelfareController;

    @Test
    @DisplayName("관리자 복지 수동 동기화 요청 시 동기화 결과를 반환한다")
    void syncWelfareData_returnsSyncResult() {
        WelfareSyncResultResponse syncResult = new WelfareSyncResultResponse(10, 9, 1, 2, 1);
        when(welfareSyncService.syncAllWelfareData()).thenReturn(syncResult);

        ResponseEntity<WelfareSyncResultResponse> response = adminWelfareController.syncWelfareData();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(syncResult);
        verify(welfareSyncService).syncAllWelfareData();
    }

    @Test
    @DisplayName("관리자 복지 상세 실패 재시도 요청 시 재시도 결과를 반환한다")
    void retryFailedDetailSync_returnsRetryResult() {
        WelfareSyncResultResponse retryResult = new WelfareSyncResultResponse(3, 2, 1, 0, 1);
        when(welfareSyncService.retryFailedDetailSync()).thenReturn(retryResult);

        ResponseEntity<WelfareSyncResultResponse> response = adminWelfareController.retryFailedDetailSync();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(retryResult);
        verify(welfareSyncService).retryFailedDetailSync();
    }
}
