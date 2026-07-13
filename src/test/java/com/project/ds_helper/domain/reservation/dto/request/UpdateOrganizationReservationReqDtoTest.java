package com.project.ds_helper.domain.reservation.dto.request;

import com.project.ds_helper.domain.reservation.entity.OrganizationReservation;
import com.project.ds_helper.domain.reservation.entity.ReservationSchedule;
import com.project.ds_helper.domain.reservation.enums.RecipientGenderType;
import com.project.ds_helper.domain.reservation.enums.ReservationStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class UpdateOrganizationReservationReqDtoTest {

    @Test
    @DisplayName("기관 예약 수정 요청은 기존 엔티티를 갱신한다")
    void toOrganizationReservation_updatesFields() {
        UpdateOrganizationReservationReqDto dto = new UpdateOrganizationReservationReqDto();
        dto.setOrganizationName("기관");
        dto.setName("홍길동");
        dto.setPhoneNumber("010-2222-3333");
        dto.setVisitDate(LocalDate.of(2026, 1, 2));
        dto.setStartTime(LocalTime.of(10, 0));
        dto.setEndTime(LocalTime.of(11, 0));
        dto.setAddress("주소2");
        dto.setRequirement(null);
        dto.setRecipientGenderType("모두");
        dto.setRecipientNumber(3);
        dto.setNote("note");

        OrganizationReservation reservation = OrganizationReservation.builder()
                .reservationStatus(ReservationStatus.REQUESTED)
                .reservationSchedule(ReservationSchedule.builder().build())
                .build();

        OrganizationReservation result = dto.toOrganizationReservation(reservation, dto);

        assertThat(result.getName()).isEqualTo("홍길동");
        assertThat(result.getRecipientGender()).isEqualTo(RecipientGenderType.BOTH);
        assertThat(result.getReservationSchedule().getEndTime()).isEqualTo(LocalTime.of(12, 0));
    }
}
