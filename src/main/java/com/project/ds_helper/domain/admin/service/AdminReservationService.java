package com.project.ds_helper.domain.admin.service;

import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.admin.repository.AdminOrganizationReservationRepository;
import com.project.ds_helper.domain.admin.repository.AdminPersonalReservationRepository;
import com.project.ds_helper.domain.reservation.dto.response.*;
import com.project.ds_helper.domain.reservation.entity.OrganizationReservation;
import com.project.ds_helper.domain.reservation.entity.PersonalReservation;
import com.project.ds_helper.domain.reservation.enums.ReservationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminReservationService {

    private final AdminPersonalReservationRepository adminPersonalReservationRepository;
    private final AdminOrganizationReservationRepository adminOrganizationReservationRepository;

    private final UserUtil userUtil;

    public Object getByRequestedReservations(int page, int size, String sort, String sortBy) {
        // 관리자 검증 임시 해제

        //userUtil.extractUserId(authentication);
        log.debug("AdminReservationService.getByRequestedReservations started. page={}, size={}, sort={}, sortBy={}", page, size, sort, sortBy);
        Pageable pageRequest = PageRequest.of(page, Math.min(size, 100), sort.equalsIgnoreCase("desc")? Sort.Direction.DESC : Sort.Direction.ASC, "createdAt");

        Page<PersonalReservation> personalReservations = adminPersonalReservationRepository.findAllByReservationStatus(ReservationStatus.REQUESTED, pageRequest);
        Page<GetPersonalReservationsByReservationStatusResDto> convertedPersonalReservations = personalReservations.map(GetPersonalReservationsByReservationStatusResDto::fromPersonalReservationToReservation);

        Page<OrganizationReservation> organizationReservations = adminOrganizationReservationRepository.findAllByReservationStatus(ReservationStatus.REQUESTED, pageRequest);
        Page<GetOrganizationReservationsByReservationStatusResDto> convertedOrganizationReservations = organizationReservations.map(GetOrganizationReservationsByReservationStatusResDto::fromOrgReservationToReservation);

        GetPersonalReservationsResDto.toDto(convertedPersonalReservations);

        HashMap<String, Object> responseDto = new HashMap<>();
        responseDto.put("personalReservations", convertedPersonalReservations);
        responseDto.put("organizationReservations", convertedOrganizationReservations);
        return responseDto;
    }

    public long getAcceptedReservationCount() {
        log.debug("AdminReservationService.getAcceptedReservationCount started.");
        long personalCount = adminPersonalReservationRepository.countByReservationStatus(ReservationStatus.COMPLETED);
        long organizationCount = adminOrganizationReservationRepository.countByReservationStatus(ReservationStatus.COMPLETED);
        return personalCount + organizationCount;
    }
}
