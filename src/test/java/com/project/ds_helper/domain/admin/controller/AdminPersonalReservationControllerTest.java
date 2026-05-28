package com.project.ds_helper.domain.admin.controller;

import com.project.ds_helper.domain.admin.dto.request.ChangePersonalReservationStatusReqDto;
import com.project.ds_helper.domain.admin.service.AdminPersonalReservationService;
import com.project.ds_helper.domain.reservation.enums.ReservationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminPersonalReservationControllerTest {

    @Mock private AdminPersonalReservationService adminPersonalReservationService;
    @Mock private Authentication authentication;

    @InjectMocks
    private AdminPersonalReservationController controller;

    @Test
    @DisplayName("개인 예약 상태 변경은 서비스 호출 후 OK를 반환한다")
    void changeReservationStatus_returnsOk() throws Exception {
        ChangePersonalReservationStatusReqDto dto =
                new ChangePersonalReservationStatusReqDto("reservation-1", ReservationStatus.COMPLETED.getKorean());

        ResponseEntity<?> response = controller.changeReservationStatus(authentication, dto);

        verify(adminPersonalReservationService).changeReservationStatus(authentication, dto);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo("OK");
    }
}

