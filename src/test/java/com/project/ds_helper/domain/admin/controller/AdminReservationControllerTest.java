package com.project.ds_helper.domain.admin.controller;

import com.project.ds_helper.domain.admin.service.AdminReservationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminReservationControllerTest {

    @Mock
    private AdminReservationService adminReservationService;

    @InjectMocks
    private AdminReservationController adminReservationController;

    @Test
    @DisplayName("예약 목록 조회는 서비스 결과를 반환한다")
    void getReservations_returnsServiceResult() {
        Map<String, Object> responseDto = Map.of("personalReservations", "p", "organizationReservations", "o");
        when(adminReservationService.getReservations(null, null, null, null, null, null, null, 0, 10, "desc", "createdAt"))
                .thenReturn(responseDto);

        ResponseEntity<?> response =
                adminReservationController.getReservations(null, null, null, null, null, null, null, 0, 10, "desc", "createdAt");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(responseDto);
    }

    @Test
    @DisplayName("수락된 예약 수 조회는 서비스 결과를 반환한다")
    void getAcceptedReservationCount_returnsServiceResult() {
        long count = 5L;
        when(adminReservationService.getAcceptedReservationCount()).thenReturn(count);

        ResponseEntity<Long> response = adminReservationController.getAcceptedReservationCount();

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(count);
    }
}
