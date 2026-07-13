package com.project.ds_helper.domain.reservation.service;

import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationQueryService reservationQueryService;
    
    public List<?> getPreReservedReservationsByDate(org.springframework.security.core.Authentication authentication, LocalDate date) {
        return reservationQueryService.getPreReservedReservationsByDate(authentication, date);
    }
}
