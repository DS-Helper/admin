package com.project.ds_helper.domain.admin.service;

import com.project.ds_helper.common.util.JwtUtil;
import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.admin.dto.request.ChangeOrganizationReservationStatusReqDto;
import com.project.ds_helper.domain.admin.repository.AdminOrganizationReservationRepository;
import com.project.ds_helper.domain.reservation.entity.OrganizationReservation;
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
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminOrganizationReservationServiceTest {

    @Mock
    private AdminOrganizationReservationRepository adminOrganizationReservationRepository;

    @Mock
    private UserUtil userUtil;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AdminOrganizationReservationService adminOrganizationReservationService;

    @Test
    @DisplayName("관리자는 기관 예약 상태를 변경할 수 있다")
    void changeReservationStatus_updatesReservationStatus() throws Exception {
        User admin = User.builder().id("admin-1").role(UserRole.ADMIN).build();
        OrganizationReservation reservation = OrganizationReservation.builder()
                .id("reservation-1")
                .reservationStatus(ReservationStatus.REQUESTED)
                .build();
        ChangeOrganizationReservationStatusReqDto dto =
                new ChangeOrganizationReservationStatusReqDto("reservation-1", ReservationStatus.COMPLETED.getKorean());

        when(userUtil.extractUserId(authentication)).thenReturn("admin-1");
        when(userUtil.findUserById("admin-1")).thenReturn(admin);
        when(adminOrganizationReservationRepository.findById("reservation-1")).thenReturn(Optional.of(reservation));

        adminOrganizationReservationService.changeReservationStatus(authentication, dto);

        assertThat(reservation.getReservationStatus()).isEqualTo(ReservationStatus.COMPLETED);
        verify(adminOrganizationReservationRepository).save(reservation);
    }

    @Test
    @DisplayName("관리자가 아니면 기관 예약 상태를 변경할 수 없다")
    void changeReservationStatus_throwsWhenRequesterIsNotAdmin() {
        User user = User.builder().id("user-1").role(UserRole.USER).build();
        ChangeOrganizationReservationStatusReqDto dto =
                new ChangeOrganizationReservationStatusReqDto("reservation-1", ReservationStatus.COMPLETED.getKorean());

        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(userUtil.findUserById("user-1")).thenReturn(user);

        assertThatThrownBy(() -> adminOrganizationReservationService.changeReservationStatus(authentication, dto))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Only Admin");
    }
}
