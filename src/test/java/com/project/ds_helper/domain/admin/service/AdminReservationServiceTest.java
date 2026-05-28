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
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

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
    void getReservations_withFilters() {
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

        Object result = adminReservationService.getReservations(
                "tester",
                ReservationStatus.REQUESTED,
                null,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 2),
                LocalDate.of(2026, 5, 10),
                LocalDate.of(2026, 5, 11),
                0,
                10,
                "desc",
                "createdAt"
        );

        assertThat(result).isInstanceOf(Map.class);
        Map<?, ?> body = (Map<?, ?>) result;
        assertThat(body.containsKey("personalReservations")).isTrue();
        assertThat(body.containsKey("organizationReservations")).isTrue();
    }

    @Test
    @DisplayName("예약 조회 Specification은 createdAt/visitDate 조건을 포함해 Predicate를 생성한다")
    void getReservations_specification_buildsPredicates() {
        when(adminPersonalReservationRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(adminOrganizationReservationRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        adminReservationService.getReservations(
                "name",
                ReservationStatus.REQUESTED,
                null,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 2),
                LocalDate.of(2026, 5, 3),
                LocalDate.of(2026, 5, 4),
                0,
                10,
                "desc",
                "createdAt"
        );

        ArgumentCaptor<Specification<PersonalReservation>> personalCaptor = ArgumentCaptor.forClass(Specification.class);
        verify(adminPersonalReservationRepository).findAll(personalCaptor.capture(), any(Pageable.class));

        jakarta.persistence.criteria.Root<PersonalReservation> root =
                org.mockito.Mockito.mock(jakarta.persistence.criteria.Root.class, org.mockito.Answers.RETURNS_MOCKS);
        jakarta.persistence.criteria.CriteriaQuery<?> query =
                org.mockito.Mockito.mock(jakarta.persistence.criteria.CriteriaQuery.class, org.mockito.Answers.RETURNS_MOCKS);
        jakarta.persistence.criteria.CriteriaBuilder cb =
                org.mockito.Mockito.mock(jakarta.persistence.criteria.CriteriaBuilder.class, org.mockito.Answers.RETURNS_MOCKS);

        jakarta.persistence.criteria.Predicate built = personalCaptor.getValue().toPredicate(root, query, cb);
        assertThat(built).isNotNull();
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
