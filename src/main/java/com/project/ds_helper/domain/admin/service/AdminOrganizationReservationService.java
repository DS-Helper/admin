package com.project.ds_helper.domain.admin.service;

import com.project.ds_helper.common.util.JwtUtil;
import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.admin.dto.request.ChangeOrganizationReservationStatusReqDto;
import com.project.ds_helper.domain.admin.repository.AdminOrganizationReservationRepository;
import com.project.ds_helper.domain.reservation.entity.OrganizationReservation;
import com.project.ds_helper.domain.reservation.enums.ReservationStatus;
import com.project.ds_helper.domain.user.entity.User;
import com.project.ds_helper.domain.user.enums.UserRole;
import com.project.ds_helper.domain.user.repository.OrganizationRepository;
import io.jsonwebtoken.security.JwkThumbprint;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminOrganizationReservationService {

    private final AdminOrganizationReservationRepository adminOrganizationReservationRepository;
    private final UserUtil userUtil;
    private final JwtUtil jwtUtil;

    public void changeReservationStatus(Authentication authentication, ChangeOrganizationReservationStatusReqDto dto) throws BadRequestException {
        String userId = userUtil.extractUserId(authentication);
        User user = userUtil.findUserById(userId);
        UserRole role = user.getRole();

        if(role != UserRole.ADMIN) throw new BadRequestException("Only Admin Can Use This API");

        String organizationReservationId = dto.organizationReservationId();
        String status = dto.status();
        log.debug("AdminOrganizationReservationService.changeReservationStatus started. organizationReservationId={}, status={}", organizationReservationId, status);

        OrganizationReservation organizationReservation = adminOrganizationReservationRepository.findById(organizationReservationId)
                .orElseThrow(()-> new IllegalArgumentException("OrganizationReservation Not Found"));

        ReservationStatus enumedStatus = ReservationStatus.findStatusByString(status);
        organizationReservation.setReservationStatus(enumedStatus);
        if(enumedStatus.equals(ReservationStatus.CANCELED)){
            organizationReservation.cancelReservation();
        }
        adminOrganizationReservationRepository.save(organizationReservation);
    }
}
