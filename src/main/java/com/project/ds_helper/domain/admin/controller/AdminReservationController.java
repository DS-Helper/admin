package com.project.ds_helper.domain.admin.controller;

import com.project.ds_helper.domain.admin.service.AdminReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/reservations")
@RequiredArgsConstructor
@Slf4j
public class AdminReservationController {

    private final AdminReservationService adminReservationService;

    @Tag(name = "관리자 > 개인 예약")
    @Operation(summary = "대기 상태의 개인 예약 전체 조회 (JWT 인증 필요)")
    @GetMapping("/requested-reservations")
    public ResponseEntity<?> getPersonalReservationsByRequestedReservations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "desc") String sort,
            @RequestParam(defaultValue = "createdAt") String sortBy
    ) {
        log.debug("AdminReservationController.getPersonalReservationsByRequestedReservations called. page={}, size={}, sort={}, sortBy={}", page, size, sort, sortBy);
        return ResponseEntity.ok(adminReservationService.getByRequestedReservations(page, size, sort, sortBy));
    }

    @Tag(name = "관리자 > 예약")
    @Operation(summary = "수락된 예약 전체 수 조회 (JWT 인증 필요)")
    @GetMapping("/accepted/count")
    public ResponseEntity<Long> getAcceptedReservationCount() {
        log.debug("AdminReservationController.getAcceptedReservationCount called.");
        return ResponseEntity.ok(adminReservationService.getAcceptedReservationCount());
    }
}
