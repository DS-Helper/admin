package com.project.ds_helper.domain.reservation.dto.request;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeleteReservationReqDtoTest {

    @Test
    @DisplayName("삭제 DTO는 식별자를 저장한다")
    void deleteDtos_holdIds() {
        DeletePersonalReservationReqDto personal = new DeletePersonalReservationReqDto("p-1");
        DeleteOrganizationReservationReqDto organization = new DeleteOrganizationReservationReqDto("o-1");

        assertThat(personal.getPersonalReservationId()).isEqualTo("p-1");
        assertThat(organization.getOrganizationReservationId()).isEqualTo("o-1");
    }
}
