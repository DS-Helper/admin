package com.project.ds_helper.domain.reservation.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import com.project.ds_helper.domain.reservation.entity.PersonalReservation;
import com.project.ds_helper.domain.reservation.enums.RecipientGenderType;
import com.project.ds_helper.domain.reservation.enums.ReservationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
@Getter
public class UpdatePersonalReservationReqDto {

    @NotBlank(message = "예약 ID는 필수 항목입니다.") // 20949013-9cbe-44e0-9916-f4e919bcef64
    @Schema(example = "20949013-9cbe-44e0-9916-f4e919bcef64")
    private String personalReservationId;

    @NotBlank(message = "예약자 성명은 필수 입력 사항입니다.")
    @Schema(example = "유나얼")
    private String name;

    @NotBlank(message = "연락처는 필수 입력 항목입니다.")
    @Pattern(regexp = "^01[016789]-\\d{3,4}-\\d{4}$") // 하이픈 있는 상태만 허용
    @Schema(example = "010-0000-0000")
    private String phoneNumber;

    @NotNull(message = "방문 날짜는 필수 입력 항목입니다.")
    @Schema(example = "2025-12-31")
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    private LocalDate visitDate;

    @NotNull(message = "시작 시간은 필수 입력 항목입니다.")
    @Schema(example = "14:22")
    @JsonSerialize(using = LocalTimeSerializer.class)
    @JsonDeserialize(using = LocalTimeDeserializer.class)
    private LocalTime startTime;

    @NotNull(message = "종료 시간은 필수 입력 항목입니다.")
    @Schema(example = "17:22")
    @JsonSerialize(using = LocalTimeSerializer.class)
    @JsonDeserialize(using = LocalTimeDeserializer.class)
    private LocalTime endTime;

    @NotBlank(message = "주소는 필수 입력 항목입니다.")
    @Schema(example = "부산광역시 서래동 서래중앙로 181번길 92, 서래아파트 101동 1304호")
    private String address;

    @Schema(example = "문서 작업좀 도와주세요.")
    private String requirement;

    @NotBlank(message = "성별은 필수 입력 항목입니다.")
    @Schema(example = "여")
    private String recipientGenderType;
@JsonFormat
    @NotNull
    @Min(value = 1)
    @Max(value = 100)
    private int recipientNumber;

    @Schema(example = "특이사항")
    private String note;

    // requirement null 체크 후 null이면 빈 문자열 반환
    public String checkRequirementNullAndReturnValue(String requirement){
        if(requirement == null){
            return "";
        }
        return requirement;
    }

    public PersonalReservation toPersonalReservation(PersonalReservation personalReservation, UpdatePersonalReservationReqDto dto){
                // 요청인 이름, 전화번호
                personalReservation.setName(dto.getName());
                personalReservation.setPhoneNumber(dto.getPhoneNumber());
                // 예약 일시 업데이트
                personalReservation.getReservationSchedule().setVisitDate(dto.getVisitDate());
                personalReservation.getReservationSchedule().setStartTime(dto.getStartTime());
                personalReservation.getReservationSchedule().setEndTime(dto.getEndTime().plusHours(1));
                // 프론트 예약 시간 제한을 위한 스케줄 업데이트
                personalReservation.getReservationSchedule().setVisitDate(dto.getVisitDate());
                personalReservation.getReservationSchedule().setStartTime(dto.getStartTime());
                personalReservation.getReservationSchedule().setEndTime(dto.getEndTime().plusHours(1));
                // 주소 및 요구사항
                personalReservation.setAddress(dto.getAddress());
                personalReservation.setRequirement(dto.checkRequirementNullAndReturnValue(dto.getRequirement()));
                // 성별, 도움 받을 사람 수, 예약 상태, 특이사항
                personalReservation.setRecipientGender(RecipientGenderType.findTypeByKorean(dto.getRecipientGenderType()));
                personalReservation.setRecipientNumber(dto.getRecipientNumber());
                personalReservation.setReservationStatus(personalReservation.getReservationStatus());
                personalReservation.setNote(dto.getNote());
        return personalReservation;
    }
}
