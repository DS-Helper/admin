package com.project.ds_helper.domain.reservation.service;

import com.project.ds_helper.common.util.JwtUtil;
import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.reservation.dto.request.CreatePersonalReservationReqDto;
import com.project.ds_helper.domain.reservation.dto.request.DeletePersonalReservationReqDto;
import com.project.ds_helper.domain.reservation.dto.request.UpdatePersonalReservationReqDto;
import com.project.ds_helper.domain.reservation.dto.response.*;
import com.project.ds_helper.domain.reservation.entity.PersonalReservation;
import com.project.ds_helper.domain.reservation.entity.ReservationSchedule;
import com.project.ds_helper.domain.reservation.enums.ReservationStatus;
import com.project.ds_helper.domain.reservation.repository.PersonalReservationRepository;
import com.project.ds_helper.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

@Service
@Slf4j
@RequiredArgsConstructor
public class PersonalReservationService {

    private final PersonalReservationRepository personalReservationRepository;
    private final UserUtil userUtil;
    private final JwtUtil jwtUtil;


    /**
     * 유저의 개인 예약 전체 조회
     * **/
    public List<GetPersonalReservationResDto> getAllPersonalReservation(Authentication authentication) {
        List<PersonalReservation> personalReservations = personalReservationRepository.findAllByUser_Id(userUtil.extractUserId(authentication));
        log.debug("personalReservations selected successfully");

        return GetPersonalReservationResDto.toDtoList(personalReservations);
    }

    /**
     * 단건 개인 예약 조회
     * **/
    public GetPersonalReservationResDto getOnePersonalReservation(Authentication authentication, String personalReservationId) {
        
        String userId = String.valueOf(authentication.getPrincipal());
        log.debug("userId : {}", userId);

        log.debug("personalReservationId : {}", personalReservationId);
    
        // 개인 예약 조회 및 응답
        PersonalReservation personalReservation = personalReservationRepository.findById(personalReservationId).
                orElseThrow(() -> new IllegalArgumentException("없는 개인 예약 입니다. 예약 ID : " + personalReservationId));
        return GetPersonalReservationResDto.toDto(personalReservation);
    }

    /**
     * 개인 에약 리스트를 상태별 조회
     * @Param status
     * @Return GetOrganizationReservationsResDto
     *
     * userId, status 기반 personalReservationRepository 조회
     * **/
    public GetPersonalReservationsResDto<?> getAllPersonalReservationByStatus(Authentication authentication, String reservationStatus, int page, int size) {
            String userId = userUtil.extractUserId(authentication);
            log.debug("userId : {}, reservationStatus : {}", userId, reservationStatus);

            // 전체 개인 예약 리스트 조회 시
            if(reservationStatus.equals("all")){
                Pageable pageable = PageRequest.of(page, size);
                Slice<PersonalReservation> personalReservations = personalReservationRepository.findAllByUser_IdOrderByCreatedAtDesc(userId, pageable);
                Slice<GetPersonalReservationsByReservationStatusResDto> convertedPersonalReservations = personalReservations.map(GetPersonalReservationsByReservationStatusResDto::fromPersonalReservationToReservation);
                return GetPersonalReservationsResDto.toDto(convertedPersonalReservations);
            } else {
                ReservationStatus _reservationStatus = ReservationStatus.findStatusByString(reservationStatus);

                Pageable pageable = PageRequest.of(page, size);
                Slice<PersonalReservation> personalReservations = personalReservationRepository.findAllByUser_IdAndReservationStatusOrderByCreatedAtDesc(userId, _reservationStatus, pageable);
                Slice<GetPersonalReservationsByReservationStatusResDto> convertedPersonalReservations = personalReservations.map(GetPersonalReservationsByReservationStatusResDto::fromPersonalReservationToReservation);
                return GetPersonalReservationsResDto.toDto(convertedPersonalReservations);
            }
    }

    /**
     * 신규 개인 예약 생성
     * **/
    public void createPersonalReservation(Authentication authentication, CreatePersonalReservationReqDto dto) throws BadRequestException {
    
        // 다른 변수들 로그 추가 작성 필요
        LocalDate visitDate = dto.getVisitDate();
        LocalTime startTime = dto.getStartTime();
        LocalTime endTime = dto.getEndTime();
        log.debug("visitDate : {}, startTime : {}, endTime : {}", visitDate, startTime, endTime);
        
        // 뒷정리 시간 1시간 추가하여 저장(조회 시에는 -1시간 하여 리턴)
        endTime = endTime.plusHours(1);
        log.debug("endTime : {}", endTime);
        
        // 신청자 정보
        String userId = userUtil.extractUserId(authentication);
        User user = userUtil.findUserById(userId);
        String userRole = user.getRole().name();
        String userType = user.getType().name();
        log.debug("userRole : {}, userType : {}", userRole, userType);

        // 이미 대기중인 예약이 있다면 예약 불가(1개만 예약 가능)
        if(personalReservationRepository.existsByUser_IdAndReservationStatus(userId, ReservationStatus.REQUESTED)){
            log.debug("Reservation Already Exists");
            throw new BadRequestException("Already Exists Requested Status Reservation");
        }

        // 개인이 아니면 예외
        if(!userType.equalsIgnoreCase("PERSONAL")){
            log.debug("only USER, ADMIN can reserve personal reservation");
            throw new BadRequestException("Only Personal User May Reserve");}
        
        // 수,금요일이 아니면 예외
        if(visitDate.getDayOfWeek().getValue() != 0 && visitDate.getDayOfWeek().getValue() != 7){
            log.debug("Only Sunday Can Make a Reservation. visitDate : {}", visitDate);
            throw new IllegalArgumentException("Only Sunday Can Make a Reservation");
        }
        
        // 10시 ~ 18시가 아니면 예외
        if(startTime.getHour() < 10 || endTime.getHour() > 18){
            log.debug("Available Time Only Between 10 to 18. startTime : {}, endTime : {}", startTime, endTime);
            throw new IllegalArgumentException("Not Proper Time. Only Between 10 and 18");
        }

        // 당일 예약 불가
        if(visitDate.isEqual(LocalDate.now(ZoneId.of("Asia/Seoul")))){
            log.debug("Not Available For Creating Today Reservation");
            throw new IllegalArgumentException("Same-day reservations are not allowed");
        }

        // 개인 예약 / 시간 엔티티 빌드
        PersonalReservation personalReservation = dto.toPersonalReservation(dto, userUtil.findUserById(userUtil.extractUserId(authentication)));
        log.debug("new personal reservation is successfully built");
        
        // 예약 스케줄
        ReservationSchedule reservationSchedule = ReservationSchedule.builder().visitDate(visitDate).startTime(startTime).endTime(endTime).build();
        log.debug("Reservation Schedule is Successfully Built");
        personalReservation.setReservationSchedule(reservationSchedule);
        // 저장
        personalReservationRepository.save(personalReservation);
        log.debug("personalReservation is successfully saved");
    }

    /**
     * 개인 예약 수정
     * **/
    public UpdatePersonalReservationResDto updatePersonalReservation(Authentication authentication, UpdatePersonalReservationReqDto dto) {

        // 다른 변수들 로그 추가 작성 필요
        LocalDate visitDate = dto.getVisitDate();
        LocalTime startTime = dto.getStartTime();
        LocalTime endTime = dto.getEndTime();
        log.debug("visitDate : {}, startTime : {}, endTime : {}", visitDate, startTime, endTime);

        // 유저 정보 획득
        String userId = String.valueOf(authentication.getPrincipal());
        log.debug("userId : {}", userId);
        
        // 예약 정보 획득
        String personalReservationId = dto.getPersonalReservationId();
        log.debug("personalReservationId : {}", personalReservationId);
    
        // 기존 개인 예약 조회
        PersonalReservation personalReservation = personalReservationRepository.findById(personalReservationId).orElseThrow(() -> new IllegalArgumentException("없는 예약 ID 입니다. : " + personalReservationId));
        if(!personalReservation.getUser().getId().equals(userId)){
            throw new IllegalArgumentException("예약자 본인만 예약 수정이 가능하니다. 예약자 ID : " + personalReservationId + "요청 유저 Id : " + userId);
        }

        // 방문 요일이 수, 금이 아닌 경우 예약 거절
        if(visitDate.getDayOfWeek().getValue() != 3 && visitDate.getDayOfWeek().getValue() != 5){
            log.debug("only wen and fri. visitDate : {}", visitDate);
            throw new IllegalArgumentException("Not Proper Day, wen, fri only");
        }

        // 예약 시간이 시작 10시, 끝 + 1시간이 18시 이후인 경우 예약 거절
        if(startTime.getHour() < 10 || endTime.getHour() > 18){
            log.debug("Available Time Only Between 10 to 18. startTime : {}, endTime : {}", startTime, endTime);
            throw new IllegalArgumentException("Not Proper Time. Only Between 10 and 18");
        }

        // 수정
        PersonalReservation updatedPersonalReservation = dto.toPersonalReservation(personalReservation, dto);
        log.debug("수정된 개인 예약 엔티티 빌드");
        
        // JPA 에서 관리 해주지만 명시적인 저장 메소드 호출
        personalReservationRepository.save(updatedPersonalReservation);
        log.debug("수정된 엔티티 저장 완료");
        
        // 저장 후 응답 객체 생성 후 반환
        UpdatePersonalReservationResDto responseDto = UpdatePersonalReservationResDto.toDto(updatedPersonalReservation);
        return responseDto;
    }
    
    /**
     * 개인 예약 취소
     * **/
    public void cancelPersonalReservation(Authentication authentication, DeletePersonalReservationReqDto dto) {

        String userId = String.valueOf(authentication.getPrincipal());
        log.debug("userId : {}", userId);

        String personalReservationId = dto.getPersonalReservationId();
        log.debug("personalReservationId : {}", personalReservationId);

        PersonalReservation personalReservation = personalReservationRepository.findById(personalReservationId).orElseThrow(() -> new IllegalArgumentException("없는 개인 예약 입니다. 예약 ID : " + personalReservationId));
        log.debug("개인 예약 조회 완료");

        personalReservation.setReservationStatus(ReservationStatus.CANCELED);
        personalReservation.cancelReservation();
        log.debug("status of reservation chagned to cancel");
        
        personalReservationRepository.save(personalReservation);
        log.debug("개인 예약 삭제 완료");
    }



}

