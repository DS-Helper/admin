package com.project.ds_helper.domain.admin.controller;

import com.project.ds_helper.domain.admin.service.AdminReservationService;
import com.project.ds_helper.domain.reservation.enums.ReservationStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/admin/reservations")
@RequiredArgsConstructor
@Slf4j
public class AdminReservationController {

    private final AdminReservationService adminReservationService;

    @Tag(name = "관리자 > 예약")
    @Operation(summary = "대기 상태의 예약 전체 조회 (JWT 인증 필요)", description = "개인 및 기관의 대기 중인 예약을 조회합니다. 검색 옵션을 지원합니다.")
    @GetMapping("/requested-reservations")
    public ResponseEntity<?> getRequestedReservations(
            @RequestParam(required = false) String requesterName,
            @RequestParam(required = false) ReservationStatus reservationStatus,
            @RequestParam(required = false) String applicantType,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "desc") String sort,
            @RequestParam(defaultValue = "createdAt") String sortBy
    ) {
        log.debug("AdminReservationController.getRequestedReservations called. name={}, status={}, type={}, dateRange={}~{}", 
                requesterName, reservationStatus, applicantType, startDate, endDate);
        return ResponseEntity.ok(adminReservationService.getRequestedReservations(
                requesterName, reservationStatus, applicantType, startDate, endDate, page, size, sort, sortBy));
    }

    @Tag(name = "관리자 > 예약")
    @Operation(summary = "수락된 예약 전체 수 조회 (JWT 인증 필요)")
    @GetMapping("/accepted/count")
    public ResponseEntity<Long> getAcceptedReservationCount() {
        log.debug("AdminReservationController.getAcceptedReservationCount called.");
        return ResponseEntity.ok(adminReservationService.getAcceptedReservationCount());
    }
}
