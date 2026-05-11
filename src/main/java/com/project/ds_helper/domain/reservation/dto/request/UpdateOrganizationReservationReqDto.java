package com.project.ds_helper.domain.reservation.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import com.project.ds_helper.domain.reservation.entity.OrganizationReservation;
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
public class UpdateOrganizationReservationReqDto {


    @NotBlank(message = "예약 ID는 필수 항목입니다.")
    @Schema(example = "20949013-9cbe-44e0-9916-f4e919bcef64")
    private String organizationReservationId;

    @NotBlank(message = "기관명은 필수 입력 사항입니다.")
    @Schema(example = "한빛 유치원")
    private String organizationName;

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
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @NotNull(message = "종료 시간은 필수 입력 항목입니다.")
    @JsonFormat(pattern = "HH:mm")
    @JsonSerialize(using = LocalTimeSerializer.class)
    @JsonDeserialize(using = LocalTimeDeserializer.class)
    @Schema(example = "17:22")
    private LocalTime endTime;

    @NotBlank(message = "주소는 필수 입력 항목입니다.")
    @Schema(example = "부산광역시 서래동 서래중앙로 181번길 92, 서래아파트 101동 1304호")
    private String address;

    @Schema(example = "서류 작업 도와주세요.")
    private String requirement;

    @NotBlank(message = "성별은 필수 입력 항목입니다.")
    @Schema(example = "모두")
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

    public OrganizationReservation toOrganizationReservation(OrganizationReservation organizationReservation, UpdateOrganizationReservationReqDto dto){

        organizationReservation.setName(dto.getName());
        organizationReservation.setPhoneNumber(dto.getPhoneNumber());
        organizationReservation.setAddress(dto.getAddress());
        organizationReservation.setRequirement(dto.checkRequirementNullAndReturnValue(dto.getRequirement()));
        // 기관 예약 자체의 예약 일시
        organizationReservation.setVisitDate(dto.getVisitDate());
        organizationReservation.setStartTime(dto.getStartTime());
        organizationReservation.setEndTime(dto.getEndTime().plusHours(1));
        // 프론트 예약 제한을 위한 예약 일시 갱신
        organizationReservation.getReservationSchedule().setVisitDate(dto.getVisitDate());
        organizationReservation.getReservationSchedule().setStartTime(dto.getStartTime());
        organizationReservation.getReservationSchedule().setEndTime(dto.getEndTime().plusHours(1));
        // 성별 체크
        organizationReservation.setRecipientGender(RecipientGenderType.findTypeByKorean(dto.getRecipientGenderType()));
        organizationReservation.setRecipientNumber(dto.getRecipientNumber());
        organizationReservation.setReservationStatus(organizationReservation.getReservationStatus());
        organizationReservation.setNote(dto.getNote());
        return organizationReservation;
    }
}
