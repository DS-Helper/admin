package com.project.ds_helper.domain.reservation.dto.response;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import com.project.ds_helper.domain.reservation.entity.PersonalReservation;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "예약 상태별 개인 예약 DTO", example = "{\"personalReservationId\":\"personal-res-001\",\"reservationHolderId\":\"user-001\",\"reservationHolder\":\"홍길동\",\"reservationPhoneNumber\":\"010-1234-5678\",\"visitDate\":\"2026-03-30\",\"startTime\":\"14:00\",\"endTime\":\"15:00\",\"address\":\"서울특별시 강남구 테헤란로 123\",\"requirement\":\"서류 작성 지원\",\"recipientGender\":\"남성\",\"recipientNumber\":1,\"note\":\"노트북 지참 예정\",\"reservationStatus\":\"REQUESTED\"}")
public record GetPersonalReservationsByReservationStatusResDto(
        String personalReservationId,
        String reservationHolderId,
        String reservationHolder,
        @JsonFormat(pattern = "^01[016789]-\\d{3,4}-\\d{4}$\n") // 하이픈 있는 상태만 허용
        String reservationPhoneNumber,
        @JsonSerialize(using = LocalDateSerializer.class)
        @JsonDeserialize(using = LocalDateDeserializer.class)
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate visitDate,
        @JsonSerialize(using = LocalTimeSerializer.class)
        @JsonDeserialize(using = LocalTimeDeserializer.class)
        @JsonFormat(pattern = "HH:mm")
        LocalTime startTime,
        @JsonSerialize(using = LocalTimeSerializer.class)
        @JsonDeserialize(using = LocalTimeDeserializer.class)
        @JsonFormat(pattern = "HH:mm")
        LocalTime endTime,
        String address,
        String requirement,
        String recipientGender,
        int recipientNumber,
        String reservationStatus,
        String note,
        @JsonSerialize(using = LocalDateSerializer.class)
        @JsonDeserialize(using = LocalDateDeserializer.class)
        @JsonFormat(pattern = "yyyy-MM-dd")
        LocalDate applicationDate
) {
    public static GetPersonalReservationsByReservationStatusResDto fromPersonalReservationToReservation(PersonalReservation personalReservation){
        return new GetPersonalReservationsByReservationStatusResDto(
                personalReservation.getId(),
                personalReservation.getUser().getId(),
                personalReservation.getName(),
                personalReservation.getPhoneNumber(),
                personalReservation.getVisitDate() != null? personalReservation.getVisitDate() : null,
                personalReservation.getStartTime() != null? personalReservation.getStartTime() : null,
                // 뒷정리 시간 1시간을 뺀 시간을 리턴(뒷정리 시간은 사용자의 예약 시간과는 무관하게 시스템적으로 추가 예약이 불가능하도록 제한한 시간)
                personalReservation.getEndTime() != null? personalReservation.getEndTime().minusHours(1) : null,
                personalReservation.getAddress(),
                personalReservation.getRequirement(),
                personalReservation.getRecipientGender().getKorean(),
                personalReservation.getRecipientNumber(),
                personalReservation.getReservationStatus().getKorean(),
                personalReservation.getNote() != null? personalReservation.getNote() : "",
                personalReservation.getCreatedAt() != null? personalReservation.getCreatedAt().toLocalDate() : null
        );
    }
}

