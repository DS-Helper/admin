package com.project.ds_helper.domain.reservation.controller;

import com.project.ds_helper.domain.reservation.service.ReservationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationControllerTest {

    @Mock
    private ReservationService reservationService;

    @InjectMocks
    private ReservationController reservationController;

    @Test
    @DisplayName("예약 선점 시간 조회는 서비스 결과를 그대로 반환한다")
    void getPreReservedReservationsByDate_returnsServiceResult() {
        LocalDate date = LocalDate.of(2026, 3, 27);
        when(reservationService.getPreReservedReservationsByDate(null, date))
                .thenAnswer(invocation -> List.of("09:00", "09:30"));

        ResponseEntity<?> response = reservationController.getPreReservedReservationsByDate(null, date);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(List.of("09:00", "09:30"));
    }
}
