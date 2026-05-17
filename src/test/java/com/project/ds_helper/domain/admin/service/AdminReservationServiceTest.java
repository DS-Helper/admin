package com.project.ds_helper.domain.admin.service;

import com.project.ds_helper.domain.admin.repository.AdminOrganizationReservationRepository;
import com.project.ds_helper.domain.admin.repository.AdminPersonalReservationRepository;
import com.project.ds_helper.domain.reservation.entity.OrganizationReservation;
import com.project.ds_helper.domain.reservation.entity.PersonalReservation;
import com.project.ds_helper.domain.reservation.entity.ReservationSchedule;
import com.project.ds_helper.domain.reservation.enums.RecipientGenderType;
import com.project.ds_helper.domain.reservation.enums.ReservationStatus;
import com.project.ds_helper.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminReservationServiceTest {

    @Mock
    private AdminPersonalReservationRepository adminPersonalReservationRepository;

    @Mock
    private AdminOrganizationReservationRepository adminOrganizationReservationRepository;

    @Mock
    private com.project.ds_helper.common.util.UserUtil userUtil;

    @InjectMocks
    private AdminReservationService adminReservationService;

    @Test
    @DisplayName("예약 목록 필터링 조회를 지원한다")
    void getRequestedReservations_withFilters() {
        PersonalReservation personal = PersonalReservation.builder()
                .id("p1")
                .name("tester")
                .user(User.builder().id("user-1").build())
                .reservationStatus(ReservationStatus.REQUESTED)
                .recipientGender(RecipientGenderType.MALE)
                .recipientNumber(1)
                .build();
        OrganizationReservation organization = OrganizationReservation.builder()
                .id("o1")
                .name("org")
                .user(User.builder().id("user-2").build())
                .reservationStatus(ReservationStatus.REQUESTED)
                .reservationSchedule(ReservationSchedule.builder()
                        .visitDate(LocalDate.of(2026, 3, 30))
                        .startTime(LocalTime.of(14, 0))
                        .endTime(LocalTime.of(15, 0))
                        .build())
                .recipientGender(RecipientGenderType.FEMALE)
                .recipientNumber(2)
                .build();

        when(adminPersonalReservationRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(personal)));
        when(adminOrganizationReservationRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(organization)));

        Object result = adminReservationService.getRequestedReservations(null, null, null, null, null, 0, 10, "desc", "createdAt");

        assertThat(result).isInstanceOf(Map.class);
        Map<?, ?> body = (Map<?, ?>) result;
        assertThat(body.containsKey("personalReservations")).isTrue();
        assertThat(body.containsKey("organizationReservations")).isTrue();
    }

    @Test
    @DisplayName("수락된(완료된) 예약 수의 합계를 반환한다")
    void getAcceptedReservationCount_returnsSum() {
        when(adminPersonalReservationRepository.countByReservationStatus(ReservationStatus.COMPLETED)).thenReturn(3L);
        when(adminOrganizationReservationRepository.countByReservationStatus(ReservationStatus.COMPLETED)).thenReturn(2L);

        long result = adminReservationService.getAcceptedReservationCount();

        assertThat(result).isEqualTo(5L);
    }
}
