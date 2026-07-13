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
class ReservationQueryServiceTest {

    @Mock private ReservationScheduleRepository reservationScheduleRepository;
    @Mock private UserUtil userUtil;
    @Mock private Authentication authentication;

    @Test
    @DisplayName("예약 슬롯이 여러 구간이면 30분 단위로 펼쳐 반환한다")
    void getPreReservedReservationsByDate_expandsToHalfHourSlots() {
        ReservationQueryService service = new ReservationQueryService(reservationScheduleRepository, userUtil);
        LocalDate date = LocalDate.of(2026, 7, 12);
        ReservationSchedule first = ReservationSchedule.builder()
                .visitDate(date)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .build();
        ReservationSchedule second = ReservationSchedule.builder()
                .visitDate(date)
                .startTime(LocalTime.of(10, 30))
                .endTime(LocalTime.of(11, 30))
                .build();
        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(reservationScheduleRepository.findByVisitDateOrderByStartTime(date)).thenReturn(List.of(first, second));

        List<String> result = (List<String>) service.getPreReservedReservationsByDate(authentication, date);

        assertThat(result).containsExactly("09:00", "09:30", "10:30", "11:00");
    }

    @Test
    @DisplayName("날짜가 없으면 오늘 날짜를 사용한다")
    void getPreReservedReservationsByDate_usesTodayWhenDateNull() {
        ReservationQueryService service = new ReservationQueryService(reservationScheduleRepository, userUtil);
        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(reservationScheduleRepository.findByVisitDateOrderByStartTime(any(LocalDate.class))).thenReturn(List.of());

        List<String> result = (List<String>) service.getPreReservedReservationsByDate(authentication, null);

        assertThat(result).isEmpty();
        verify(reservationScheduleRepository).findByVisitDateOrderByStartTime(any(LocalDate.class));
    }
}
