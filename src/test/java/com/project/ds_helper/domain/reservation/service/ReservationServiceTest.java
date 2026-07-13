package com.project.ds_helper.domain.reservation.service;

import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.reservation.entity.ReservationSchedule;
import com.project.ds_helper.domain.reservation.repository.ReservationScheduleRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock private UserUtil userUtil;
    @Mock private ReservationScheduleRepository reservationScheduleRepository;
    @Mock private Authentication authentication;


    @Test
    @DisplayName("날짜별 예약 슬롯을 30분 단위로 반환한다")
    void getPreReservedReservationsByDate_returnsHalfHourSlots() {
        LocalDate date = LocalDate.of(2026, 3, 27);
        ReservationSchedule schedule = ReservationSchedule.builder()
                .visitDate(date)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .build();

        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(reservationScheduleRepository.findByVisitDateOrderByStartTime(date)).thenReturn(List.of(schedule));

        List<?> result = new ReservationQueryService(reservationScheduleRepository, userUtil).getPreReservedReservationsByDate(authentication, date);

        assertThat(result).isEqualTo(List.of("09:00", "09:30"));
    }

    @Test
    @DisplayName("예약 스케줄이 없으면 빈 목록을 반환한다")
    void getPreReservedReservationsByDate_returnsEmptyWhenNoSchedule() {
        LocalDate date = LocalDate.of(2026, 3, 27);

        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(reservationScheduleRepository.findByVisitDateOrderByStartTime(date)).thenReturn(List.of());

        List<?> result = new ReservationQueryService(reservationScheduleRepository, userUtil).getPreReservedReservationsByDate(authentication, date);

        assertThat(result).isEmpty();
        verify(reservationScheduleRepository).findByVisitDateOrderByStartTime(date);
    }

    @Test
    @DisplayName("ReservationService는 조회 책임을 ReservationQueryService에 위임한다")
    void reservationService_delegatesToQueryService() {
        LocalDate date = LocalDate.of(2026, 3, 27);
        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(reservationScheduleRepository.findByVisitDateOrderByStartTime(date)).thenReturn(List.of());

        List<?> result = new ReservationService(new ReservationQueryService(reservationScheduleRepository, userUtil)).getPreReservedReservationsByDate(authentication, date);

        assertThat(result).isEmpty();
        verify(reservationScheduleRepository).findByVisitDateOrderByStartTime(date);
    }

    @Test
    @DisplayName("날짜가 없으면 오늘 날짜 기준으로 조회한다")
    void getPreReservedReservationsByDate_usesTodayWhenDateNull() {
        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(reservationScheduleRepository.findByVisitDateOrderByStartTime(any(LocalDate.class))).thenReturn(List.of());

        List<?> result = new ReservationQueryService(reservationScheduleRepository, userUtil).getPreReservedReservationsByDate(authentication, null);

        assertThat(result).isEmpty();
        verify(reservationScheduleRepository).findByVisitDateOrderByStartTime(any(LocalDate.class));
    }
}
