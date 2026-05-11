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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminReservationService {

    private final AdminPersonalReservationRepository adminPersonalReservationRepository;
    private final AdminOrganizationReservationRepository adminOrganizationReservationRepository;

    private final UserUtil userUtil;

    public Object getRequestedReservations(
            String requesterName, 
            ReservationStatus reservationStatus, 
            String applicantType, 
            LocalDate startDate, 
            LocalDate endDate, 
            int page, 
            int size, 
            String sort, 
            String sortBy
    ) {
        log.debug("AdminReservationService.getRequestedReservations started.");
        Pageable pageRequest = PageRequest.of(page, Math.min(size, 100), 
                sort.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, sortBy);

        HashMap<String, Object> responseDto = new HashMap<>();

        // 1. 개인 예약 필터링 및 조회
        if (applicantType == null || applicantType.equalsIgnoreCase("PERSONAL")) {
            Specification<PersonalReservation> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                if (requesterName != null) predicates.add(cb.like(root.get("name"), "%" + requesterName + "%"));
                if (reservationStatus != null) predicates.add(cb.equal(root.get("reservationStatus"), reservationStatus));
                if (startDate != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate.atStartOfDay()));
                if (endDate != null) predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate.atTime(23, 59, 59)));
                return cb.and(predicates.toArray(new Predicate[0]));
            };
            Page<PersonalReservation> personalReservations = adminPersonalReservationRepository.findAll(spec, pageRequest);
            Page<GetPersonalReservationsByReservationStatusResDto> convertedPersonalReservations = 
                    personalReservations.map(GetPersonalReservationsByReservationStatusResDto::fromPersonalReservationToReservation);
            responseDto.put("personalReservations", convertedPersonalReservations);
        }

        // 2. 기관 예약 필터링 및 조회
        if (applicantType == null || applicantType.equalsIgnoreCase("ORGANIZATION")) {
            Specification<OrganizationReservation> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                if (requesterName != null) predicates.add(cb.like(root.get("name"), "%" + requesterName + "%"));
                if (reservationStatus != null) predicates.add(cb.equal(root.get("reservationStatus"), reservationStatus));
                if (startDate != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate.atStartOfDay()));
                if (endDate != null) predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate.atTime(23, 59, 59)));
                return cb.and(predicates.toArray(new Predicate[0]));
            };
            Page<OrganizationReservation> organizationReservations = adminOrganizationReservationRepository.findAll(spec, pageRequest);
            Page<GetOrganizationReservationsByReservationStatusResDto> convertedOrganizationReservations = 
                    organizationReservations.map(GetOrganizationReservationsByReservationStatusResDto::fromOrgReservationToReservation);
            responseDto.put("organizationReservations", convertedOrganizationReservations);
        }

        return responseDto;
    }

    public long getAcceptedReservationCount() {
        log.debug("AdminReservationService.getAcceptedReservationCount started.");
        long personalCount = adminPersonalReservationRepository.countByReservationStatus(ReservationStatus.COMPLETED);
        long organizationCount = adminOrganizationReservationRepository.countByReservationStatus(ReservationStatus.COMPLETED);
        return personalCount + organizationCount;
    }
}
