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
import com.project.ds_helper.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalTime;

@Slf4j
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
@Getter
public class CreatePersonalReservationReqDto {

    @NotBlank(message = "예약자 성명은 필수 입력 사항입니다.")
    @Schema(example = "유나얼")
    private String name;

    @NotBlank(message = "연락처는 필수 입력 항목입니다.")
    @Pattern(
            regexp = "^(0\\d{1,2})-\\d{3,4}-\\d{4}$",
            message = "전화번호 형식을 확인해 주세요."
    )
    @Schema(examples = {"010-4875-1738", "02-762-2513"})
    private String phoneNumber;

    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @NotNull(message = "방문 날짜는 필수 입력 항목입니다.")
    @Schema(example = "2025-12-31")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate visitDate;

    @JsonSerialize(using = LocalTimeSerializer.class)
    @JsonDeserialize(using = LocalTimeDeserializer.class)
    @NotNull(message = "시작 시간은 필수 입력 항목입니다.")
    @Schema(example = "14:22")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime startTime;

    @JsonSerialize(using = LocalTimeSerializer.class)
    @JsonDeserialize(using = LocalTimeDeserializer.class)
    @NotNull(message = "종료 시간은 필수 입력 항목입니다.")
    @Schema(example = "17:22")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime endTime;

    @NotBlank(message = "주소는 필수 입력 항목입니다.")
    @Schema(example = "부산광역시 서래동 서래중앙로 181번길 92, 서래아파트 101동 1304호")
    private String address;

    @Schema(example = "문서 작업좀 도와주세요.")
    private String requirement;
    
    @NotBlank(message = "성별은 필수 입력 항목입니다.")
    @Schema(example = "여")
    private String recipientGenderType;

    @NotNull
    @Min(value = 1)
    @Max(value = 100)
    @Schema(example = "1")
    private int recipientNumber;

    @Schema(example = "특이 사항")
    private String note;

    // requirement null 체크 후 null이면 빈 문자열 반환
    public String checkRequirementNullAndReturnValue(String requirement){
        if(requirement == null){
            return "";
        }
        return requirement;
    }

    public PersonalReservation toPersonalReservation(CreatePersonalReservationReqDto dto, User user){

                log.info("Address : {}, Name : {}, Requirement : {}, RecipientNumber : {}, PhoneNumber : {}, visitDate : {}. StartTime : {}, EndTime : {}, RecipientGenderType : {} "
                        , dto.getAddress(), dto.getName(), dto.getRequirement(), dto.getRecipientNumber(), dto.getPhoneNumber()
                        , dto.getVisitDate(), dto.getStartTime(), dto.getEndTime(), dto.getRecipientGenderType());
        return PersonalReservation.builder()
                .name(dto.getName())
                .phoneNumber(dto.getPhoneNumber())
                .address(dto.getAddress())
                .requirement(dto.checkRequirementNullAndReturnValue(dto.getRequirement()))
                .user(user)
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
