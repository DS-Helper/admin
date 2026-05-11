package com.project.ds_helper.domain.reservation.controller;

import com.project.ds_helper.common.enums.SwaggerTagName;
import com.project.ds_helper.domain.reservation.dto.request.CreateOrganizationReservationReqDto;
import com.project.ds_helper.domain.reservation.dto.request.DeleteOrganizationReservationReqDto;
import com.project.ds_helper.domain.reservation.dto.request.UpdateOrganizationReservationReqDto;
import com.project.ds_helper.domain.reservation.dto.response.GetOrganizationReservationResDto;
import com.project.ds_helper.domain.reservation.dto.response.GetOrganizationReservationsResDto;
import com.project.ds_helper.domain.reservation.service.OrganizationReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/organization-reservations")
public class OrganizationReservationController {

    private final OrganizationReservationService organizationReservationService;

    @Tag(name = SwaggerTagName.ORGANIZATION_RESERVATION)
    @Operation(summary = "단건 기관 예약 조회 (JWT 인증 필요)")
    @GetMapping("/{organizationReservationId}")
    public ResponseEntity<GetOrganizationReservationResDto> getOneOrganizationReservation(
            Authentication authentication,
            @Parameter(example = "d2d3f2df-d585-4db8-ba8f-775ff41b3b0c")
            @PathVariable("organizationReservationId") String organizationReservationId
    ) {
        log.debug("OrganizationReservationController.getOneOrganizationReservation called. organizationReservationId={}", organizationReservationId);
        return new ResponseEntity<>(organizationReservationService.getOneOrganizationReservation(authentication, organizationReservationId), HttpStatus.OK);
    }

    @Tag(name = SwaggerTagName.ORGANIZATION_RESERVATION)
    @Operation(summary = "예약 상태별 조회 (JWT 인증 필요)")
    @GetMapping("/status")
    public ResponseEntity<GetOrganizationReservationsResDto<?>> getAllOrganizationReservationByReservationStatus(
            Authentication authentication,
            @RequestParam(value = "reservationStatus", defaultValue = "all") String reservationStatus,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sort", defaultValue = "desc") String sort
    ) {
        log.debug("OrganizationReservationController.getAllOrganizationReservationByReservationStatus called. reservationStatus={}, page={}, size={}, sort={}",
                reservationStatus, page, size, sort);
        return ResponseEntity.ok(organizationReservationService.getAllOrganizationReservationByReservationStatus(authentication, reservationStatus, page, size, sort));
    }

    @Tag(name = SwaggerTagName.ORGANIZATION_RESERVATION)
    @Operation(summary = "신규 기관 예약 생성 (JWT 인증 필요)")
    @PostMapping("")
    public ResponseEntity<Void> createOrganizationReservation(
            Authentication authentication,
            @RequestBody @Valid CreateOrganizationReservationReqDto dto
    ) throws BadRequestException {
        log.debug("OrganizationReservationController.createOrganizationReservation called. visitDate={}, startTime={}", dto.getVisitDate(), dto.getStartTime());
        organizationReservationService.createOrganizationReservation(authentication, dto);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @Tag(name = SwaggerTagName.ORGANIZATION_RESERVATION)
    @Operation(summary = "기관 예약 수정 (JWT 인증 필요)")
    @PutMapping("")
    public ResponseEntity<Void> updateOrganizationReservation(
            Authentication authentication,
            @RequestBody @Valid UpdateOrganizationReservationReqDto dto
    ) {
        log.debug("OrganizationReservationController.updateOrganizationReservation called. organizationReservationId={}", dto.getOrganizationReservationId());
        organizationReservationService.updateOrganizationReservation(authentication, dto);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @Tag(name = SwaggerTagName.ORGANIZATION_RESERVATION)
    @Operation(summary = "기관 예약 취소 (JWT 인증 필요)")
    @PatchMapping("")
    public ResponseEntity<Void> cancelOrganizationReservation(
            Authentication authentication,
            @RequestBody @Valid DeleteOrganizationReservationReqDto dto
    ) {
        log.debug("OrganizationReservationController.cancelOrganizationReservation called. organizationReservationId={}", dto.getOrganizationReservationId());
        organizationReservationService.cancelOrganizationReservation(authentication, dto);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
