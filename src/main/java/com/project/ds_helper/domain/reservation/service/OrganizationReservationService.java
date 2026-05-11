package com.project.ds_helper.domain.reservation.service;

import com.project.ds_helper.common.util.JwtUtil;
import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.reservation.dto.request.CreateOrganizationReservationReqDto;
import com.project.ds_helper.domain.reservation.dto.request.DeleteOrganizationReservationReqDto;
import com.project.ds_helper.domain.reservation.dto.request.UpdateOrganizationReservationReqDto;
import com.project.ds_helper.domain.reservation.dto.response.*;
import com.project.ds_helper.domain.reservation.entity.OrganizationReservation;
import com.project.ds_helper.domain.reservation.entity.ReservationSchedule;
import com.project.ds_helper.domain.reservation.enums.ReservationStatus;
import com.project.ds_helper.domain.reservation.repository.OrganizationReservationRepository;
import com.project.ds_helper.domain.reservation.repository.ReservationScheduleRepository;
import com.project.ds_helper.domain.user.entity.User;
import com.project.ds_helper.domain.user.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrganizationReservationService {

    private final OrganizationReservationRepository organizationReservationRepository;
    private final ReservationScheduleRepository reservationScheduleRepository;
    private final UserUtil userUtil;

    /**
     * 단건 기관 예약 조회
     * **/
    public GetOrganizationReservationResDto getOneOrganizationReservation(Authentication authentication, String organizationReservationId) {

        String userId = String.valueOf(authentication.getPrincipal());
        log.debug("userId : {}", userId);

        log.debug("organizationReservationId : {}", organizationReservationId);

        // 기관 단건 예약 조회 및 응답
        return GetOrganizationReservationResDto.toDto(organizationReservationRepository.findById(organizationReservationId).
                orElseThrow(() -> new IllegalArgumentException("없는 기관 예약 입니다. 예약 ID : " + organizationReservationId)));
    }

    /**
     * 기관 에약 리스트를 상태별 조회
     * @Param reservationStatus
     * @Return List<GetOrganizationReservationResDto>
     *
     * userId, reservationStatus 기반 organizationRepository 조회
     * **/
    public GetOrganizationReservationsResDto<?> getAllOrganizationReservationByReservationStatus(Authentication authentication, String reservationStatus, int page, int size, String sort) {
        String userId = userUtil.extractUserId(authentication);
        log.debug("userId : {}, reservationStatus : {}", userId, reservationStatus);

        if(reservationStatus.equals("all")){
            Pageable pageable = PageRequest.of(page, size);
            Slice<OrganizationReservation> organizationReservations = organizationReservationRepository.findAllByUser_IdOrderByCreatedAtDesc(userId, pageable);
            Slice<GetOrganizationReservationsByReservationStatusResDto> convertedOrganizationReservations = organizationReservations.map(GetOrganizationReservationsByReservationStatusResDto::fromOrgReservationToReservation);
            return GetOrganizationReservationsResDto.toDto(convertedOrganizationReservations);
        } else {
            ReservationStatus _reservationStatus = ReservationStatus.findStatusByString(reservationStatus);

            Pageable pageable = PageRequest.of(page, size);
            Slice<OrganizationReservation> organizationReservations = organizationReservationRepository.findAllByUser_IdAndReservationStatusOrderByCreatedAtDesc(userId, _reservationStatus, pageable);
            Slice<GetOrganizationReservationsByReservationStatusResDto> convertedOrganizationReservations = organizationReservations.map(GetOrganizationReservationsByReservationStatusResDto::fromOrgReservationToReservation);
            return GetOrganizationReservationsResDto.toDto(convertedOrganizationReservations);
        }
    }


    /**
     * 신규 기관 예약 생성
     * **/
    public void createOrganizationReservation(Authentication authentication, CreateOrganizationReservationReqDto dto) throws BadRequestException {

        // 다른 변수들 로그 추가 작성 필요
        LocalDate visitDate = dto.getVisitDate();
        LocalTime startTime = dto.getStartTime();
        LocalTime endTime = dto.getEndTime();
        log.debug("visitDate : {}, startTime : {}, endTime : {}", visitDate, startTime, endTime);

        endTime = endTime.plusHours(1);
        log.debug("endTime : {}", endTime);

        String userId = userUtil.extractUserId(authentication);
        User user = userUtil.findUserById(userId);
        String userRole = user.getRole().name();
        String userType = user.getType().name();
        log.debug("userRole : {}, userType : {}", userRole, userType);

        // 이미 대기중인 예약이 있다면 예약 불가(1개만 예약 가능)
        if(organizationReservationRepository.existsByUser_IdAndReservationStatus(userId, ReservationStatus.REQUESTED)){
            throw new BadRequestException("Already Exists Requested Status Reservation");
        }

        // 기관이 아니면 예외
        if(!userType.equalsIgnoreCase("ORGANIZATION")){throw new BadRequestException("Only Organization May Reserve");}

        // 방문 요일이 수, 금이 아닌 경우 예약 거절
        if(visitDate.getDayOfWeek().getValue() != 0 && visitDate.getDayOfWeek().getValue() != 7){
             log.debug("Only Sunday Can Make a Reservation. visitDate : {}", visitDate);
             throw new IllegalArgumentException("Only Sunday Can Make a Reservation");
        }

        // 예약 시간이 시작 10시, 끝 + 1시간이 18시 이후인 경우 예약 거절
        if(startTime.getHour() < 10 || endTime.getHour() > 18){
            log.debug("Available Time Only Between 10 to 18. startTime : {}, endTime : {}", startTime, endTime);
            throw new IllegalArgumentException("Not Proper Time. Only Between 10 and 18");
        }

        // 당일 예약 불가
        if(visitDate.isEqual(LocalDate.now(ZoneId.of("Asia/Seoul")))){
            log.debug("Not Allowed For Today Reservation");
            throw new IllegalArgumentException("Same-day reservations are not allowed");
        }

        // 기예약된 시간과 중복되면 예약 불가
        reservationScheduleRepository.existsOverlap(visitDate, startTime, endTime);

        // 유저 정보 획득
        // 개인 예약 엔티티 빌드
        OrganizationReservation organizationReservation = dto.toOrganizationReservation(dto, userUtil.findUserById(userUtil.extractUserId(authentication)));
        log.debug("신규 기관 예약 엔티티 빌드 완료");

        ReservationSchedule reservationSchedule = ReservationSchedule.builder().visitDate(visitDate).startTime(startTime).endTime(endTime).build();
        log.debug("reservationSchedule is Successfully Built");
        organizationReservation.setReservationSchedule(reservationSchedule);

        // 저장 후 응답 객체 생성 후 반환
        organizationReservationRepository.save(organizationReservation);
        log.debug("New Organization Reservation Successfully Saved");
    }

    /**
     * 기존 예약을 수정한다.
     *
     * 봉사 일정은 수,금 10~17시만 가능하다.
     *
     * 사용자가 요청한 시간의 끝 시간에 뒷정리를 위한 1시간을 더하여 최종 일정은 + 1시간 된 시간으로 저장 된다.
     * **/
    public void updateOrganizationReservation(Authentication authentication, UpdateOrganizationReservationReqDto dto) {

        // 다른 변수들 로그 추가 작성 필요
        LocalDate visitDate = dto.getVisitDate();
        LocalTime startTime = dto.getStartTime();
        LocalTime endTime = dto.getEndTime();
        log.debug("visitDate : {}, startTime : {}, endTime : {}", visitDate, startTime, endTime);

        // 사용자가 예약한 시간에 뒷정리를 위한 1시간을 추가한다.
        endTime = endTime.plusHours(1);
        log.debug("endTime : {}", endTime);

        // 예약 수정을 요청한 유저 정보 획득
        String userId = String.valueOf(authentication.getPrincipal());
        log.debug("userId : {}", userId);

        // 수정할 새로운 예약 정보 획득
        String organizationReservationId = dto.getOrganizationReservationId();
        log.debug("organizationReservationId : {}", organizationReservationId);

        // 수정을 위해 기존 개인 예약 조회
        OrganizationReservation organizationReservation = organizationReservationRepository.findById(organizationReservationId).orElseThrow(() -> new IllegalArgumentException("없는 예약 ID 입니다. : " + organizationReservationId));
        if(!organizationReservation.getUser().getId().equals(userId)){
            throw new IllegalArgumentException("예약자 본인만 예약 수정이 가능하니다. 예약 ID : " + organizationReservationId + "요청 유저 Id : " + userId);
        }
        
        // 방문 요일이 수, 금이 아닌 경우 예약 거절
        if(visitDate.getDayOfWeek().getValue() != 3 && visitDate.getDayOfWeek().getValue() != 5){
            log.debug("only wen, fri available. visitDate : {}", visitDate);
            throw new IllegalArgumentException("Not Proper Day, wen, fri only");
        }
        
        // 예약 시간이 시작 10시, 끝 + 1시간이 18시 이후인 경우 예약 거절
        if(startTime.getHour() < 10 || endTime.getHour() > 18){
            log.debug("Available Time Only Between 10 to 18. startTime : {}, endTime : {}", startTime, endTime);
            throw new IllegalArgumentException("Not Proper Time. Only Between 10 and 18");
        }

        // 수정
        OrganizationReservation updatedOrganizationReservation = dto.toOrganizationReservation(organizationReservation, dto);
        log.debug("수정된 개인 예약 엔티티 빌드");

        // JPA 에서 관리 해주지만 명시적인 저장 메소드 호출
        organizationReservationRepository.save(updatedOrganizationReservation);
        log.debug("수정된 엔티티 저장 완료");
    }

    /**
     * 기관 예약 취소
     *
     * 예약의 상태를 취소로 변경 후 저장한다. (삭제하지 않고 기록을 남김)
     * 영구 저장(추후 일정 기간이 지난 예약은 삭제하도록 해도 무방할듯)
     * **/
    public void cancelOrganizationReservation(Authentication authentication, DeleteOrganizationReservationReqDto dto) {
    
        // 취소를 요청한 유저 아이디 조회
        String userId = String.valueOf(authentication.getPrincipal());
        log.debug("userId : {}", userId);
        
        // dto에서 기관id 조회
        String organizationReservationId = dto.getOrganizationReservationId();
        log.debug("organizationReservationId : {}", organizationReservationId);
        
        // 취소를 위해 기존 예약 조회
        OrganizationReservation organizationReservation = organizationReservationRepository.findById(organizationReservationId).orElseThrow(() -> new IllegalArgumentException("없는 기관 예약 입니다. 예약 ID : " + organizationReservationId));
        log.debug("기관 예약 조회 완료");
        
        // 예약 취소
        organizationReservation.setReservationStatus(ReservationStatus.CANCELED);
        organizationReservation.cancelReservation();
        
        // 예약 취소 된 상태로 저장 (삭제하지 않음)
        organizationReservationRepository.save(organizationReservation);
        log.debug("기관 예약 삭제 완료");
    }
    

}

