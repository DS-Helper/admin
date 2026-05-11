package com.project.ds_helper.domain.reservation.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import com.project.ds_helper.domain.reservation.entity.OrganizationReservation;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "예약 상태별 통합 예약 DTO", example = "{\"organizationReservationId\":\"org-res-001\",\"organizationName\":\"행복복지관\",\"reservationHolderId\":\"user-001\",\"reservationHolder\":\"홍길동\",\"reservationPhoneNumber\":\"010-1234-5678\",\"visitDate\":\"2026-03-30\",\"startTime\":\"14:00\",\"endTime\":\"15:00\",\"address\":\"서울특별시 강남구 테헤란로 123\",\"requirement\":\"서류 작성 지원\",\"note\":\"노트북 지참 예정\",\"reservationStatus\":\"REQUESTED\"}")
public record GetReservationsByReservationStatusResDto(
         String organizationReservationId,
         String organizationName,
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
         String note

        ) {
//    @Builder
//    static class Reservation{
//        private String organizationReservationId;
//        private String organizationName;
//        private String reservationHolderId;
//        private String reservationHolder;
//        @JsonFormat(pattern = "^01[016789]-\\d{3,4}-\\d{4}$\n") // 하이픈 있는 상태만 허용
//        private String reservationPhoneNumber;
//        @JsonSerialize(using = LocalDateSerializer.class)
//        @JsonDeserialize(using = LocalDateDeserializer.class)
//        @JsonFormat(pattern = "yyyy-MM-dd")
//        private LocalDate visitDate;
//        @JsonSerialize(using = LocalTimeSerializer.class)
//        @JsonDeserialize(using = LocalTimeDeserializer.class)
//        @JsonFormat(pattern = "HH:mm")
//        private LocalTime startTime;
//        @JsonSerialize(using = LocalTimeSerializer.class)
//        @JsonDeserialize(using = LocalTimeDeserializer.class)
//        @JsonFormat(pattern = "HH:mm")
//        private LocalTime endTime;
//        private String address;
//        private String requirement;
//        private String recipientGender;
//        private int recipientNumber;
//        private String reservationStatus;
//
//
//    }

        public static GetReservationsByReservationStatusResDto fromOrgReservationToReservation(OrganizationReservation organizationReservation){
            return new GetReservationsByReservationStatusResDto(
                    organizationReservation.getId(),
                    organizationReservation.getOrganizationName(),
                    organizationReservation.getUser().getId(),
                    organizationReservation.getName(),
                    organizationReservation.getPhoneNumber(),
                    organizationReservation.getReservationSchedule().getVisitDate(),
                    organizationReservation.getReservationSchedule().getStartTime(),
                    organizationReservation.getReservationSchedule().getEndTime().minusHours(1),
                    organizationReservation.getAddress(),
                    organizationReservation.getRequirement(),
                    organizationReservation.getRecipientGender().getKorean(),
                    organizationReservation.getRecipientNumber(),
                    organizationReservation.getReservationStatus().getKorean(),
                    organizationReservation.getNote() != null? organizationReservation.getNote() : ""
                    );
        }

//        public static PageResDto toPageResDto(Page<PersonalReservation> per, Page<OrganizationReservation> org){
//            int page = per.getNumber(); // 0-based (Slice일 땐 호출자가 0 고정으로 넣어도 OK)
//            int size = per.getSize();
//            long totalElements = per.getTotalElements();  // Slice면 -1 또는 0
//            int totalPages = per.getTotalPages(); // Slice면 -1
//            boolean first = per.isFirst();
//            boolean last = per.isLast();
//            boolean hasNext = per.hasNext();
//            boolean hasPrevious = per.hasPrevious();
//            Sort sort = per.getSort();    // 정렬 정보 에코백
//        }
}
