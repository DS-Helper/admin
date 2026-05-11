package com.project.ds_helper.domain.reservation.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import com.project.ds_helper.domain.reservation.entity.OrganizationReservation;
import com.project.ds_helper.domain.reservation.entity.PersonalReservation;
import com.project.ds_helper.domain.reservation.enums.RecipientGenderType;
import com.project.ds_helper.domain.reservation.enums.ReservationStatus;
import com.project.ds_helper.domain.user.entity.User;
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
public class CreateOrganizationReservationReqDto {

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

    @NotNull
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @Schema(example = "2025-12-31")
    private LocalDate visitDate;

    @NotNull
    @Schema(example = "14:22")
    @JsonSerialize(using = LocalTimeSerializer.class)
    @JsonDeserialize(using = LocalTimeDeserializer.class)
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @NotNull
    @JsonSerialize(using = LocalTimeSerializer.class)
    @JsonDeserialize(using = LocalTimeDeserializer.class)
    @JsonFormat(pattern = "HH:mm")
    @Schema(example = "17:22")
    private LocalTime endTime;

    @NotBlank(message = "주소는 필수 입력 항목입니다.")
    @Schema(example = "부산광역시 서래동 서래중앙로 181번길 92, 서래아파트 101동 1304호")
    private String address;

    @Schema(example = "서류 작업 도와주세요.")
    private String requirement;

    @NotBlank(message = "성별은 필수 입력 항목입니다.")
    @Schema(example = "남")
    private String recipientGenderType;

    @NotNull
    @Min(value = 1)
    @Max(value = 100)
    @Schema(example = "1")
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

    public OrganizationReservation toOrganizationReservation(CreateOrganizationReservationReqDto dto, User user){

        return OrganizationReservation.builder()
                .organizationName(dto.getOrganizationName())
                .name(dto.getName())
                .phoneNumber(dto.getPhoneNumber())
                .address(dto.getAddress())
                .requirement(dto.checkRequirementNullAndReturnValue(dto.getRequirement()))
                .user(user)
                // 예약 일시
                .visitDate(dto.getVisitDate())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime().plusHours(1))
                // 성별 체크
                .recipientGender(RecipientGenderType.findTypeByKorean(dto.getRecipientGenderType()))
                .recipientNumber(dto.getRecipientNumber())
                .reservationStatus(ReservationStatus.REQUESTED)
                .note(dto.getNote() != null? dto.getNote() : "")
                .build();
    }
}
