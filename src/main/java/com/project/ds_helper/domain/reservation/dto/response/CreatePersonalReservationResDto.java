package com.project.ds_helper.domain.reservation.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import com.project.ds_helper.domain.reservation.entity.PersonalReservation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class CreatePersonalReservationResDto {

    @NotNull
    @Schema(example = "20949013-9cbe-44e0-9916-f4e919bcef64")
    private String personalReservationId;

    @NotNull
    @Schema(example = "20949013-9cbe-44e0-9916-f4e919bcef64")
    private String reservationHolderId;

    @NotNull
    @Schema(example = "박효신")
    private String reservationHolder;

    @NotNull
    @Schema(example = "010-0000-0000")
    @JsonFormat(pattern = "^01[016789]-\\d{3,4}-\\d{4}$\n") // 하이픈 있는 상태만 허용
    private String reservationPhoneNumber;

    @NotNull
    @Schema(example = "2025-12-31")
    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private String visitDate;


    @NotNull
    @Schema(example = "14:22")
    @JsonSerialize(using = LocalTimeSerializer.class)
    @JsonDeserialize(using = LocalTimeDeserializer.class)
    @JsonFormat(pattern = "HH:mm")
    private String startTime;

    @NotNull
    @Schema(example = "17:22")
    @JsonSerialize(using = LocalTimeSerializer.class)
    @JsonDeserialize(using = LocalTimeDeserializer.class)
    @JsonFormat(pattern = "HH:mm")
    private String endTime;

    @NotBlank(message = "주소는 필수 입력 항목입니다.")
    @Schema(example = "부산광역시 서래동 서래중앙로 181번길 92, 서래아파트 101동 1304호")
    private String address;

    @Schema(example = "서류 작업 도와주세요.")
    private String requirement;

    @NotBlank(message = "성별은 필수 입력 항목입니다.")
    @Schema(example = "남")
    private String recipientGender;

    @NotNull
    @Min(value = 1)
    @Max(value = 100)
    @Schema(example = "1")
    private int recipientNumber;

    @NotNull
    @Schema(example = "대기")
    private String reservationStatus;

    public static CreatePersonalReservationResDto toDto(PersonalReservation personalReservation){
        return CreatePersonalReservationResDto.builder()
                .personalReservationId(personalReservation.getId())
                .reservationHolderId(personalReservation.getUser().getId())
                .reservationHolder(personalReservation.getName())
                .reservationPhoneNumber(personalReservation.getPhoneNumber())
                .visitDate(personalReservation.getReservationSchedule().getVisitDate() != null? personalReservation.getReservationSchedule().getVisitDate().toString() : null)
                .startTime(personalReservation.getReservationSchedule().getStartTime().toString())
                .endTime(personalReservation.getReservationSchedule().getEndTime().toString())
                .address(personalReservation.getAddress())
                .requirement(personalReservation.getRequirement())
                .recipientGender(personalReservation.getRecipientGender().getKorean())
                .recipientNumber(personalReservation.getRecipientNumber())
                .reservationStatus(personalReservation.getReservationStatus().getKorean())
                .build();
    }


}
