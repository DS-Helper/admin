package com.project.ds_helper.domain.reservation.controller;

import com.project.ds_helper.common.enums.SwaggerTagName;
import com.project.ds_helper.domain.reservation.dto.request.CreatePersonalReservationReqDto;
import com.project.ds_helper.domain.reservation.dto.request.DeletePersonalReservationReqDto;
import com.project.ds_helper.domain.reservation.dto.request.UpdatePersonalReservationReqDto;
import com.project.ds_helper.domain.reservation.dto.response.GetPersonalReservationResDto;
import com.project.ds_helper.domain.reservation.dto.response.GetPersonalReservationsResDto;
import com.project.ds_helper.domain.reservation.dto.response.UpdatePersonalReservationResDto;
import com.project.ds_helper.domain.reservation.service.PersonalReservationService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
@RequestMapping("/personal-reservations")
public class PersonalReservationController {

    private final PersonalReservationService personalReservationService;

    @Tag(name = SwaggerTagName.PERSONAL_RESERVATION)
    @Operation(summary = "단건 개인 예약 조회 (JWT 인증 필요)")
    @GetMapping("/{personalReservationId}")
    public ResponseEntity<GetPersonalReservationResDto> getOnePersonalReservation(
            Authentication authentication,
            @PathVariable("personalReservationId") String personalReservationId
    ) {
        log.debug("PersonalReservationController.getOnePersonalReservation called. personalReservationId={}", personalReservationId);
        return new ResponseEntity<>(personalReservationService.getOnePersonalReservation(authentication, personalReservationId), HttpStatus.OK);
    }

    @Tag(name = SwaggerTagName.PERSONAL_RESERVATION)
    @Operation(summary = "예약 상태별 조회 (JWT 인증 필요)")
    @ApiResponse(
            responseCode = "200",
            description = "예약 상태별 개인 예약 목록 조회 성공",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = GetPersonalReservationsResDto.class),
                    examples = @ExampleObject(
                            value = """
                                    {
                                      "content": [
                                        {
                                          "personalReservationId": "personal-res-001",
                                          "reservationHolderId": "user-001",
                                          "reservationHolder": "홍길동",
                                          "reservationPhoneNumber": "010-1234-5678",
                                          "visitDate": "2026-03-30",
                                          "startTime": "14:00",
                                          "endTime": "15:00",
                                          "address": "서울특별시 강남구 테헤란로 123",
                                          "requirement": "서류 작성 지원",
                                          "recipientGender": "남성",
                                          "recipientNumber": 1,
                                          "reservationStatus": "요청됨",
                                          "note": "노트북 지참 예정"
                                        }
                                      ],
                                      "number": 0,
                                      "size": 10,
                                      "first": true,
                                      "last": false,
                                      "hasNext": true
                                    }
                                    """
                    )
            )
    )
    @GetMapping("/status")
    public ResponseEntity<GetPersonalReservationsResDto<?>> getAllPersonalReservationByStatus(
            Authentication authentication,
            @RequestParam(value = "reservationStatus", defaultValue = "all", required = false) String reservationStatus,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        log.debug("PersonalReservationController.getAllPersonalReservationByStatus called. reservationStatus={}, page={}, size={}", reservationStatus, page, size);
        return ResponseEntity.ok(personalReservationService.getAllPersonalReservationByStatus(authentication, reservationStatus, page, size));
    }

    @Tag(name = SwaggerTagName.PERSONAL_RESERVATION)
    @Operation(summary = "신규 개인 예약 생성 (JWT 인증 필요)")
    @PostMapping("")
    public ResponseEntity<Void> createPersonalReservation(
            Authentication authentication,
            @RequestBody CreatePersonalReservationReqDto dto
    ) throws BadRequestException {
        log.debug("PersonalReservationController.createPersonalReservation called. visitDate={}, startTime={}", dto.getVisitDate(), dto.getStartTime());
        personalReservationService.createPersonalReservation(authentication, dto);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @Tag(name = SwaggerTagName.PERSONAL_RESERVATION)
    @Operation(summary = "개인 예약 수정 (JWT 인증 필요)")
    @PutMapping("")
    public ResponseEntity<UpdatePersonalReservationResDto> updatePersonalReservation(
            Authentication authentication,
            @RequestBody @Valid UpdatePersonalReservationReqDto dto
    ) {
        log.debug("PersonalReservationController.updatePersonalReservation called. personalReservationId={}", dto.getPersonalReservationId());
        return new ResponseEntity<>(personalReservationService.updatePersonalReservation(authentication, dto), HttpStatus.CREATED);
    }

    @Tag(name = SwaggerTagName.PERSONAL_RESERVATION)
    @Operation(summary = "개인 예약 취소 (JWT 인증 필요)")
    @PatchMapping("")
    public ResponseEntity<Void> cancelPersonalReservation(
            Authentication authentication,
            @RequestBody @Valid DeletePersonalReservationReqDto dto
    ) {
        log.debug("PersonalReservationController.cancelPersonalReservation called. personalReservationId={}", dto.getPersonalReservationId());
        personalReservationService.cancelPersonalReservation(authentication, dto);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
