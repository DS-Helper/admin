package com.project.ds_helper.domain.admin.service;

import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.admin.dto.request.ChangePersonalReservationStatusReqDto;
import com.project.ds_helper.domain.admin.repository.AdminOrganizationReservationRepository;
import com.project.ds_helper.domain.admin.repository.AdminPersonalReservationRepository;
import com.project.ds_helper.domain.reservation.dto.response.GetPersonalReservationsByReservationStatusResDto;
import com.project.ds_helper.domain.reservation.dto.response.GetPersonalReservationsResDto;
import com.project.ds_helper.domain.reservation.entity.PersonalReservation;
import com.project.ds_helper.domain.reservation.enums.ReservationStatus;
import com.project.ds_helper.domain.user.entity.User;
import com.project.ds_helper.domain.user.enums.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.*;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminPersonalReservationService {

    private final AdminPersonalReservationRepository adminPersonalReservationRepository;
    private final AdminOrganizationReservationRepository adminOrganizationReservationRepository;
    private final UserUtil userUtil;

    public Object getByRequestedReservations(Authentication authentication, int page, int size, String sort, String sortBy) {
        String userId = userUtil.extractUserId(authentication);
        log.debug("AdminPersonalReservationService.getByRequestedReservations started. page={}, size={}, sort={}, sortBy={}", page, size, sort, sortBy);
        Pageable pageRequest = PageRequest.of(page, Math.min(size, 100), sort.equalsIgnoreCase("desc")? Sort.Direction.DESC : Sort.Direction.ASC, "createdAt");
        Page<PersonalReservation> personalReservations = adminPersonalReservationRepository.findAllByReservationStatus(ReservationStatus.REQUESTED, pageRequest);
        Slice<GetPersonalReservationsByReservationStatusResDto> convertedPersonalReservations = personalReservations.map(GetPersonalReservationsByReservationStatusResDto::fromPersonalReservationToReservation);
//        Page<OrganizationReservation> organizationReservations = adminOrganizationReservationRepository.findAllByReservationStatus(ReservationStatus.REQUESTED, pageRequest);
        return GetPersonalReservationsResDto.toDto(convertedPersonalReservations);
    }

    /**
     * 관리자 기능
     * 개인 예약의 상태를 변경한다.
     * @Param String status : 변경할 상태
     *
     * 관리자가 아닌 유저는 제한된다.
     * 이미 같은 상태를 중복 변경할 경우 허용
     **/
    public void changeReservationStatus(Authentication authentication, ChangePersonalReservationStatusReqDto dto) throws BadRequestException {
           String userId = userUtil.extractUserId(authentication);
           User user = userUtil.findUserById(userId);

           String personalReservationId = dto.personalReservationId();
           String status = dto.status();
        log.debug("AdminPersonalReservationService.changeReservationStatus started. personalReservationId={}, status={}", personalReservationId, status);

           // 관리자가 아니면 예외
           if(user.getRole() != UserRole.ADMIN){
            log.debug("AdminPersonalReservationService.changeReservationStatus denied. role={}", user.getRole());
               throw new BadRequestException("Only Admin May Use This Function");
           }

           PersonalReservation personalReservation = adminPersonalReservationRepository.findById(personalReservationId).orElseThrow(() -> new IllegalArgumentException("PersonalReservation Not Found. PersonalReservationId : " + personalReservationId));
           ReservationStatus enumedStatus = ReservationStatus.findStatusByString(status);
        log.debug("AdminPersonalReservationService.changeReservationStatus resolved status. enumedStatus={}", enumedStatus);
           personalReservation.setReservationStatus(enumedStatus);
           if(enumedStatus.equals(ReservationStatus.CANCELED)){
               personalReservation.cancelReservation();
           }

           adminPersonalReservationRepository.save(personalReservation);
    }
}
