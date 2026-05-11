package com.project.ds_helper.domain.reservation.controller;

import com.project.ds_helper.common.enums.SwaggerTagName;
import com.project.ds_helper.domain.reservation.service.ReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/reservations")
@Tag(name = SwaggerTagName.RESERVATION)
public class ReservationController {

    private final ReservationService reservationService;

    @Operation(summary = "날짜별 기예약 내역 조회 (JWT 인증 필요)")
    @GetMapping("/pre-reserved")
    public ResponseEntity<?> getPreReservedReservationsByDate(
            Authentication authentication,
            @RequestParam(name = "date") LocalDate date
    ) {
        log.debug("ReservationController.getPreReservedReservationsByDate called. date={}", date);
        return ResponseEntity.ok(reservationService.getPreReservedReservationsByDate(authentication, date));
    }
}
