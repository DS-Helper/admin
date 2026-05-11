package com.project.ds_helper.domain.reservation.controller;

import com.project.ds_helper.domain.reservation.dto.request.DeletePersonalReservationReqDto;
import com.project.ds_helper.domain.reservation.dto.response.GetPersonalReservationResDto;
import com.project.ds_helper.domain.reservation.dto.response.GetPersonalReservationsResDto;
import com.project.ds_helper.domain.reservation.dto.response.UpdatePersonalReservationResDto;
import com.project.ds_helper.domain.reservation.service.PersonalReservationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonalReservationControllerTest {

    @Mock
    private PersonalReservationService personalReservationService;

    @InjectMocks
    private PersonalReservationController personalReservationController;

    @Test
    @DisplayName("개인 예약 단건 조회는 서비스 결과를 반환한다")
    void getOnePersonalReservation_returnsServiceResult() {
        GetPersonalReservationResDto dto = GetPersonalReservationResDto.builder().personalReservationId("p-1").build();
        when(personalReservationService.getOnePersonalReservation(null, "p-1")).thenReturn(dto);

        ResponseEntity<GetPersonalReservationResDto> response = personalReservationController.getOnePersonalReservation(null, "p-1");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(dto);
    }

    @Test
    @DisplayName("상태별 개인 예약 조회는 서비스 결과를 반환한다")
    void getAllPersonalReservationByStatus_returnsServiceResult() {
        GetPersonalReservationsResDto<?> dto = GetPersonalReservationsResDto.toDto(new org.springframework.data.domain.SliceImpl<>(List.of()));
        when(personalReservationService.getAllPersonalReservationByStatus(null, "all", 0, 10))
                .thenAnswer(invocation -> dto);

        ResponseEntity<GetPersonalReservationsResDto<?>> response =
                personalReservationController.getAllPersonalReservationByStatus(null, "all", 0, 10);

        assertThat(response.getBody()).isEqualTo(dto);
    }

    @Test
    @DisplayName("개인 예약 취소는 서비스 호출 후 204를 반환한다")
    void cancelPersonalReservation_returnsNoContent() {
        DeletePersonalReservationReqDto dto = new DeletePersonalReservationReqDto();
        ReflectionTestUtils.setField(dto, "personalReservationId", "p-1");

        ResponseEntity<Void> response = personalReservationController.cancelPersonalReservation(null, dto);

        verify(personalReservationService).cancelPersonalReservation(null, dto);
        assertThat(response.getStatusCode().value()).isEqualTo(204);
    }
}
