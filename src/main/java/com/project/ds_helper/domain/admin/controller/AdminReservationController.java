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
    @Operation(
            summary = "예약 전체 조회 (JWT 인증 필요)",
            description = ""
                    + "- 개인/기관 예약을 한 API로 통합 조회합니다.\n"
                    + "- reservationStatus가 null이면 전체 상태를 조회합니다.\n"
                    + "- applicantType이 null이면 PERSONAL+ORGANIZATION을 모두 조회합니다.\n"
                    + "- 날짜 필터는 2종을 지원합니다: 신청일(createdAt), 방문일(visitDate).\n"
                    + "- 응답은 personalReservations / organizationReservations로 분리되어 내려갑니다."
    )
    @GetMapping()
    public ResponseEntity<?> getReservations(
            @RequestParam(required = false) String requesterName,
            @RequestParam(required = false) ReservationStatus reservationStatus,
            @RequestParam(required = false) String applicantType,
            @RequestParam(required = false) LocalDate createdAtStartDate,
            @RequestParam(required = false) LocalDate createdAtEndDate,
            @RequestParam(required = false) LocalDate visitDateStart,
            @RequestParam(required = false) LocalDate visitDateEnd,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "desc") String sort,
            @RequestParam(defaultValue = "createdAt") String sortBy
    ) {
        log.debug("AdminReservationController.getReservations called. name={}, status={}, type={}, createdAtRange={}~{}, visitDateRange={}~{}",
                requesterName, reservationStatus, applicantType, createdAtStartDate, createdAtEndDate, visitDateStart, visitDateEnd);
        return ResponseEntity.ok(adminReservationService.getReservations(
                requesterName, reservationStatus, applicantType, createdAtStartDate, createdAtEndDate, visitDateStart, visitDateEnd, page, size, sort, sortBy));
    }

    @Tag(name = "관리자 > 예약")
    @Operation(
            summary = "수락된 예약 전체 수 조회 (JWT 인증 필요)",
            description = "reservation_status=COMPLETED(완료)인 예약 건수를 개인+기관 합산으로 반환합니다."
    )
    @GetMapping("/accepted/count")
    public ResponseEntity<Long> getAcceptedReservationCount() {
        log.debug("AdminReservationController.getAcceptedReservationCount called.");
        return ResponseEntity.ok(adminReservationService.getAcceptedReservationCount());
    }
}
