package com.project.ds_helper.domain.reservation.service;

import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.reservation.dto.response.GetPreReservedReservationByDateResDto;
import com.project.ds_helper.domain.reservation.entity.ReservationSchedule;
import com.project.ds_helper.domain.reservation.repository.OrganizationReservationRepository;
import com.project.ds_helper.domain.reservation.repository.PersonalReservationRepository;
import com.project.ds_helper.domain.reservation.repository.ReservationScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReservationService {

    private final PersonalReservationRepository personalReservationRepository;
    private final OrganizationReservationRepository organizationReservationRepository;
    private final ReservationScheduleRepository reservationScheduleRepository;
    private final UserUtil userUtil;
    
    /**
     * 일자별 기예약된 예약 내역 조회
     * **/
    public List<?> getPreReservedReservationsByDate(Authentication authentication, LocalDate date) {
        userUtil.extractUserId(authentication);
        // 일자가 비어있다면 오늘 기준 탐색
        if(date == null){
            date = LocalDate.now(ZoneId.of("Asia/Seoul"));
        }
        log.debug("date : {}", date);
        // 일자 기반 탐색, 시작 시간 기준 오름차순 정렬
        List<ReservationSchedule> schedules = reservationScheduleRepository.findByVisitDateOrderByStartTime(date);
        List<String> reservedSlots = new ArrayList<>();
        // 시:분 형식 적용
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

        if(schedules.isEmpty()){
            log.debug("schedules is empty");
        }else{
            log.debug("schedules does exist. schedules count : {}", schedules.size());
        }

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
