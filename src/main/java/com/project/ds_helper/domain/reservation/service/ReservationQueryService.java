package com.project.ds_helper.domain.reservation.service;

import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.reservation.entity.ReservationSchedule;
import com.project.ds_helper.domain.reservation.repository.ReservationScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationQueryService {

    private final ReservationScheduleRepository reservationScheduleRepository;
    private final UserUtil userUtil;

    @Transactional(readOnly = true)
    public List<?> getPreReservedReservationsByDate(Authentication authentication, LocalDate date) {
        userUtil.extractUserId(authentication);
        if (date == null) {
            date = LocalDate.now(ZoneId.of("Asia/Seoul"));
        }
        List<ReservationSchedule> schedules = reservationScheduleRepository.findByVisitDateOrderByStartTime(date);
        List<String> reservedSlots = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        for (ReservationSchedule s : schedules) {
            LocalTime time = s.getStartTime();
            while (time.isBefore(s.getEndTime())) {
                reservedSlots.add(time.format(formatter));
                time = time.plusMinutes(30);
            }
        }
        return reservedSlots;
    }
}
