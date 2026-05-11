package com.project.ds_helper.domain.admin.controller;

import com.project.ds_helper.common.enums.SwaggerTagName;
import com.project.ds_helper.domain.admin.dto.request.ChangeOrganizationReservationStatusReqDto;
import com.project.ds_helper.domain.admin.service.AdminOrganizationReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/organization-reservation")
@RequiredArgsConstructor
@Slf4j
@Tag(name = SwaggerTagName.ADMIN_ORGANIZATION_RESERVATION)
public class AdminOrganizationReservationController {

    private final AdminOrganizationReservationService adminOrganizationReservationService;

    @Operation(summary = "관리자가 기관 예약 상태를 변경하는 API (JWT 인증 필요)")
    @PatchMapping("/status")
    public ResponseEntity<?> changeReservationStatus(
            @Parameter(
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE),
                    schema = @Schema(implementation = ChangeOrganizationReservationStatusReqDto.class)
            )
            Authentication authentication,
            @RequestBody @Valid ChangeOrganizationReservationStatusReqDto dto
    ) throws BadRequestException {
        adminOrganizationReservationService.changeReservationStatus(authentication, dto);
        return ResponseEntity.ok("OK");
    }
}
