package com.project.ds_helper.domain.reservation.controller;

import com.project.ds_helper.domain.reservation.dto.request.DeleteOrganizationReservationReqDto;
import com.project.ds_helper.domain.reservation.dto.response.GetOrganizationReservationResDto;
import com.project.ds_helper.domain.reservation.dto.response.GetOrganizationReservationsResDto;
import com.project.ds_helper.domain.reservation.service.OrganizationReservationService;
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
class OrganizationReservationControllerTest {

    @Mock
    private OrganizationReservationService organizationReservationService;

    @InjectMocks
    private OrganizationReservationController organizationReservationController;

    @Test
    @DisplayName("기관 예약 단건 조회는 서비스 결과를 반환한다")
    void getOneOrganizationReservation_returnsServiceResult() {
        GetOrganizationReservationResDto dto = GetOrganizationReservationResDto.builder().organizationReservationId("o-1").build();
        when(organizationReservationService.getOneOrganizationReservation(null, "o-1")).thenReturn(dto);

        ResponseEntity<GetOrganizationReservationResDto> response =
                organizationReservationController.getOneOrganizationReservation(null, "o-1");

        assertThat(response.getBody()).isEqualTo(dto);
    }

    @Test
    @DisplayName("상태별 기관 예약 조회는 서비스 결과를 반환한다")
    void getAllOrganizationReservationByReservationStatus_returnsServiceResult() {
        GetOrganizationReservationsResDto<?> dto = GetOrganizationReservationsResDto.toDto(new org.springframework.data.domain.SliceImpl<>(List.of()));
        when(organizationReservationService.getAllOrganizationReservationByReservationStatus(null, "all", 0, 10, "desc"))
                .thenAnswer(invocation -> dto);

        ResponseEntity<GetOrganizationReservationsResDto<?>> response =
                organizationReservationController.getAllOrganizationReservationByReservationStatus(null, "all", 0, 10, "desc");

        assertThat(response.getBody()).isEqualTo(dto);
    }

    @Test
    @DisplayName("기관 예약 취소는 서비스 호출 후 204를 반환한다")
    void cancelOrganizationReservation_returnsNoContent() {
        DeleteOrganizationReservationReqDto dto = new DeleteOrganizationReservationReqDto();
        ReflectionTestUtils.setField(dto, "organizationReservationId", "o-1");

        ResponseEntity<Void> response = organizationReservationController.cancelOrganizationReservation(null, dto);

        verify(organizationReservationService).cancelOrganizationReservation(null, dto);
        assertThat(response.getStatusCode().value()).isEqualTo(204);
    }
}
