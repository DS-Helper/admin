package com.project.ds_helper.domain.admin.service;

import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.admin.repository.AdminUserRepository;
import com.project.ds_helper.domain.reservation.entity.PersonalReservation;
import com.project.ds_helper.domain.reservation.repository.PersonalReservationRepository;
import com.project.ds_helper.domain.user.entity.User;
import com.project.ds_helper.domain.user.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock
    private AdminUserRepository adminUserRepository;

    @Mock
    private PersonalReservationRepository personalReservationRepository;

    @Mock
    private UserUtil userUtil;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AdminUserService adminUserService;

    @Test
    @DisplayName("가입한 사용자 수를 조회한다")
    void userCount_returnsCountMap() {
        when(adminUserRepository.countByRole("USER")).thenReturn(12);

        Object result = adminUserService.userCount(authentication);

        assertThat(result).isInstanceOf(Map.class);
        assertThat((Map<String, Integer>) result).containsEntry("userCount", 12);
    }

    @Test
    @DisplayName("관리자는 이름으로 사용자 예약 정보를 조회할 수 있다")
    void getUserInfoByName_returnsReservationsForAdmin() {
        User admin = User.builder().id("admin-1").role(UserRole.ADMIN).build();
        PersonalReservation reservation = PersonalReservation.builder()
                .id("reservation-1")
                .name("tester")
                .phoneNumber("010-1111-1111")
                .visitDate(LocalDate.of(2026, 4, 17))
                .startTime(LocalTime.of(10, 0))
                .endTime(LocalTime.of(11, 0))
                .recipientGender(com.project.ds_helper.domain.reservation.enums.RecipientGenderType.BOTH)
                .recipientNumber(1)
                .reservationStatus(com.project.ds_helper.domain.reservation.enums.ReservationStatus.REQUESTED)
                .build();

        when(userUtil.extractUserId(authentication)).thenReturn("admin-1");
        when(userUtil.findUserById("admin-1")).thenReturn(admin);
        when(personalReservationRepository.findAllByNameOrderByCreatedAtDesc("tester"))
                .thenReturn(List.of(reservation));

        Object result = adminUserService.getUserInfoByName("tester", authentication);

        assertThat(result).isInstanceOf(List.class);
        assertThat((List<?>) result).hasSize(1);
    }

    @Test
    @DisplayName("관리자가 아니면 이름 기반 사용자 조회를 할 수 없다")
    void getUserInfoByName_throwsWhenRequesterIsNotAdmin() {
        User user = User.builder().id("user-1").role(UserRole.USER).build();

        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(userUtil.findUserById("user-1")).thenReturn(user);

        assertThatThrownBy(() -> adminUserService.getUserInfoByName("tester", authentication))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Only Admin");
    }
}
