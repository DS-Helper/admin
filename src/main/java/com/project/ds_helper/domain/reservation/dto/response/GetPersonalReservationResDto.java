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
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Schema(description = "개인 예약 조회 응답 DTO", example = "{\"personalReservationId\":\"personal-res-001\",\"reservationHolderId\":\"user-001\",\"reservationHolder\":\"홍길동\",\"reservationPhoneNumber\":\"010-1234-5678\",\"visitDate\":\"2026-03-30\",\"startTime\":\"14:00\",\"endTime\":\"15:00\",\"address\":\"서울특별시 강남구 테헤란로 123\",\"requirement\":\"서류 작성 지원\",\"recipientGender\":\"남성\",\"recipientNumber\":1,\"note\":\"노트북 지참 예정\",\"reservationStatus\":\"REQUESTED\"}")
public class GetPersonalReservationResDto {

    private String personalReservationId;

    private String reservationHolderId;

    private String reservationHolder;

    @JsonFormat(pattern = "^01[016789]-\\d{3,4}-\\d{4}$\n") // 하이픈 있는 상태만 허용
    private String reservationPhoneNumber;

    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate visitDate;

    @JsonSerialize(using = LocalTimeSerializer.class)
    @JsonDeserialize(using = LocalTimeDeserializer.class)
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @JsonSerialize(using = LocalTimeSerializer.class)
    @JsonDeserialize(using = LocalTimeDeserializer.class)
    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    private String address;

    private String requirement;

    private String recipientGender;

    private int recipientNumber;

    private String reservationStatus;

    private String note;

    public static GetPersonalReservationResDto toDto(PersonalReservation personalReservation){
        return GetPersonalReservationResDto.builder()
                .personalReservationId(personalReservation.getId())
                .personalReservationId(personalReservation.getUser().getId())
                .reservationHolder(personalReservation.getName())
                .reservationPhoneNumber(personalReservation.getPhoneNumber())
                .visitDate(personalReservation.getVisitDate() != null? personalReservation.getVisitDate() : null)
                .startTime(personalReservation.getStartTime() != null? personalReservation.getStartTime() : null)
                .endTime(personalReservation.getEndTime() != null? personalReservation.getEndTime().minusHours(1) : null)
                .address(personalReservation.getAddress())
                .requirement(personalReservation.getRequirement())
                .recipientGender(personalReservation.getRecipientGender().getKorean())
                .recipientNumber(personalReservation.getRecipientNumber())
                .reservationStatus(personalReservation.getReservationStatus().getKorean())
                .note(personalReservation.getNote() != null? personalReservation.getNote() : "")
                .build();
    }

    public static List<GetPersonalReservationResDto> toDtoList(List<PersonalReservation> personalReservations){
        if(personalReservations.isEmpty()) {
            return new ArrayList<GetPersonalReservationResDto>();
        }
        return personalReservations.stream().map(GetPersonalReservationResDto::toDto).toList();


    }

}
