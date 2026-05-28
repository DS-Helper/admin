package com.project.ds_helper.domain.admin;

import com.project.ds_helper.domain.admin.dto.request.ChangeOrganizationReservationStatusReqDto;
import com.project.ds_helper.domain.admin.dto.request.ChangePersonalReservationStatusReqDto;
import com.project.ds_helper.domain.admin.entity.Admin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class AdminDtoEntityTest {

    @Test
    @DisplayName("Admin 저장 전 UUID를 생성한다")
    void adminPrePersist_generatesUuid() {
        Admin admin = new Admin();

        ReflectionTestUtils.invokeMethod(admin, "prePersistGenerateId");

        assertThat(admin.getId()).isNotBlank();
    }

    @Test
    @DisplayName("ChangePersonalReservationStatusReqDto logFields는 예외 없이 동작한다")
    void changePersonalReservationStatusReqDto_logFields() {
        ChangePersonalReservationStatusReqDto dto = new ChangePersonalReservationStatusReqDto("p1", "완료");
        dto.logFields();
        assertThat(dto.personalReservationId()).isEqualTo("p1");
    }

    @Test
    @DisplayName("ChangeOrganizationReservationStatusReqDto logFields는 예외 없이 동작한다")
    void changeOrganizationReservationStatusReqDto_logFields() {
        ChangeOrganizationReservationStatusReqDto dto = new ChangeOrganizationReservationStatusReqDto("o1", "완료");
        dto.logFields();
        assertThat(dto.organizationReservationId()).isEqualTo("o1");
    }
}

