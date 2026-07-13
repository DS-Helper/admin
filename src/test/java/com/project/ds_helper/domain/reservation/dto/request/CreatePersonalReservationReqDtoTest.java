package com.project.ds_helper.domain.reservation.dto.request;

import com.project.ds_helper.domain.reservation.entity.PersonalReservation;
import com.project.ds_helper.domain.reservation.enums.RecipientGenderType;
import com.project.ds_helper.domain.reservation.enums.ReservationStatus;
import com.project.ds_helper.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class CreatePersonalReservationReqDtoTest {

    @Test
    @DisplayName("개인 예약 요청은 엔티티로 변환된다")
    void toPersonalReservation_mapsFields() {
        CreatePersonalReservationReqDto dto = new CreatePersonalReservationReqDto();
        dto.setName("유나얼");
        dto.setPhoneNumber("010-1111-2222");
        dto.setVisitDate(LocalDate.of(2026, 1, 1));
        dto.setStartTime(LocalTime.of(14, 0));
        dto.setEndTime(LocalTime.of(15, 0));
        dto.setAddress("주소");
        dto.setRequirement(null);
        dto.setRecipientGenderType("여");
        dto.setRecipientNumber(2);
        dto.setNote(null);

        PersonalReservation reservation = dto.toPersonalReservation(dto, User.builder().id("user-1").build());

        assertThat(reservation.getRecipientGender()).isEqualTo(RecipientGenderType.FEMALE);
        assertThat(reservation.getReservationStatus()).isEqualTo(ReservationStatus.REQUESTED);
        assertThat(reservation.getEndTime()).isEqualTo(LocalTime.of(16, 0));
    }
}
