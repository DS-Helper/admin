package com.project.ds_helper.domain.admin.service;

import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.admin.dto.request.ChangePersonalReservationStatusReqDto;
import com.project.ds_helper.domain.admin.repository.AdminOrganizationReservationRepository;
import com.project.ds_helper.domain.admin.repository.AdminPersonalReservationRepository;
import com.project.ds_helper.domain.reservation.entity.PersonalReservation;
import com.project.ds_helper.domain.reservation.enums.ReservationStatus;
import com.project.ds_helper.domain.user.entity.User;
import com.project.ds_helper.domain.user.enums.UserRole;
import org.apache.coyote.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminPersonalReservationServiceTest {

    @Mock
    private AdminPersonalReservationRepository adminPersonalReservationRepository;

    @Mock
    private AdminOrganizationReservationRepository adminOrganizationReservationRepository;

    @Mock
    private UserUtil userUtil;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AdminPersonalReservationService adminPersonalReservationService;

    @Test
    @DisplayName("요청 상태 개인 예약 목록을 조회한다")
    void getByRequestedReservations_returnsRequestedReservations() {
        PersonalReservation reservation = PersonalReservation.builder()
                .id("reservation-1")
                .user(User.builder().id("user-1").build())
                .name("tester")
                .phoneNumber("010-1111-1111")
                .visitDate(LocalDate.of(2026, 4, 17))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .recipientGender(com.project.ds_helper.domain.reservation.enums.RecipientGenderType.BOTH)
                .recipientNumber(1)
                .reservationStatus(ReservationStatus.REQUESTED)
                .build();

        when(userUtil.extractUserId(authentication)).thenReturn("admin-1");
        when(adminPersonalReservationRepository.findAllByReservationStatus(org.mockito.ArgumentMatchers.eq(ReservationStatus.REQUESTED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(reservation)));

        Object result = adminPersonalReservationService.getByRequestedReservations(authentication, 0, 10, "desc", "createdAt");

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("관리자는 개인 예약 상태를 변경할 수 있다")
    void changeReservationStatus_updatesReservationStatus() throws Exception {
        User admin = User.builder().id("admin-1").role(UserRole.ADMIN).build();
        PersonalReservation reservation = PersonalReservation.builder()
                .id("reservation-1")
                .reservationStatus(ReservationStatus.REQUESTED)
                .build();
        ChangePersonalReservationStatusReqDto dto =
                new ChangePersonalReservationStatusReqDto("reservation-1", ReservationStatus.COMPLETED.getKorean());

        when(userUtil.extractUserId(authentication)).thenReturn("admin-1");
        when(userUtil.findUserById("admin-1")).thenReturn(admin);
        when(adminPersonalReservationRepository.findById("reservation-1")).thenReturn(Optional.of(reservation));

        adminPersonalReservationService.changeReservationStatus(authentication, dto);

        assertThat(reservation.getReservationStatus()).isEqualTo(ReservationStatus.COMPLETED);
        verify(adminPersonalReservationRepository).save(reservation);
    }

    @Test
    @DisplayName("관리자가 아니면 개인 예약 상태를 변경할 수 없다")
    void changeReservationStatus_throwsWhenRequesterIsNotAdmin() {
        User user = User.builder().id("user-1").role(UserRole.USER).build();
        ChangePersonalReservationStatusReqDto dto =
                new ChangePersonalReservationStatusReqDto("reservation-1", ReservationStatus.COMPLETED.getKorean());

        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(userUtil.findUserById("user-1")).thenReturn(user);

        assertThatThrownBy(() -> adminPersonalReservationService.changeReservationStatus(authentication, dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only Admin");
    }
}
