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
import com.project.ds_helper.domain.reservation.entity.OrganizationReservation;
import com.project.ds_helper.domain.reservation.entity.PersonalReservation;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Schema(description = "기관 예약 생성 응답 DTO", example = "{\"organizationReservationId\":\"org-res-001\",\"organizationName\":\"행복복지관\",\"reservationHolderId\":\"user-001\",\"reservationHolder\":\"홍길동\",\"reservationPhoneNumber\":\"010-1234-5678\",\"visitDate\":\"2026-03-30\",\"startTime\":\"14:00\",\"endTime\":\"15:00\",\"address\":\"서울특별시 강남구 테헤란로 123\",\"requirement\":\"서류 작성 지원\",\"note\":\"노트북 지참 예정\",\"reservationStatus\":\"REQUESTED\"}")
public class CreateOrganizationReservationResDto {

    private String organizationReservationId;

    private String organizationName;

    private String reservationHolderId;

    private String reservationHolder;

    @JsonFormat(pattern = "^01[016789]-\\d{3,4}-\\d{4}$\n") // 하이픈 있는 상태만 허용
    private String reservationPhoneNumber;

    @JsonSerialize(using = LocalDateSerializer.class)
    @JsonDeserialize(using = LocalDateDeserializer.class)
    @JsonFormat(pattern = "yyyy-MM-dd")
    private String visitDate;

    @JsonSerialize(using = LocalTimeSerializer.class)
    @JsonDeserialize(using = LocalTimeDeserializer.class)
    @JsonFormat(pattern = "HH:mm")
    private String startTime;

    @JsonSerialize(using = LocalTimeSerializer.class)
    @JsonDeserialize(using = LocalTimeDeserializer.class)
    @JsonFormat(pattern = "HH:mm")
    private String endTime;

    private String address;

    private String requirement;

    private String recipientGender;

    private int recipientNumber;

    private String reservationStatus;

    public static CreateOrganizationReservationResDto toDto(OrganizationReservation organizationReservation){
        return CreateOrganizationReservationResDto.builder()
                .organizationReservationId(organizationReservation.getId())
                .organizationName(organizationReservation.getOrganizationName())
                .reservationHolder(organizationReservation.getName())
                .reservationPhoneNumber(organizationReservation.getPhoneNumber())
                .address(organizationReservation.getAddress())
                .requirement(organizationReservation.getRequirement())
                .recipientGender(organizationReservation.getRecipientGender().getKorean())
                .recipientNumber(organizationReservation.getRecipientNumber())
                .reservationStatus(organizationReservation.getReservationStatus().getKorean())
                .build();
    }
}
