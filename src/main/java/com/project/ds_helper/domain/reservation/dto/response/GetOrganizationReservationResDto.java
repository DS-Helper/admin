package com.project.ds_helper.domain.reservation.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers;
import com.fasterxml.jackson.databind.ser.std.DateSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import com.project.ds_helper.domain.reservation.entity.OrganizationReservation;
import com.project.ds_helper.domain.reservation.entity.PersonalReservation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class GetOrganizationReservationResDto {

    @NotNull
    @Schema(example = "20949013-9cbe-44e0-9916-f4e919bcef64")
    private String organizationReservationId;

    @NotNull
    @Schema(example = "한빛 유치원")
    private String organizationName;

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
    private LocalDate visitDate;

    @NotNull
    @Schema(example = "14:22")
    @JsonSerialize(using = LocalTimeSerializer.class)
    @JsonDeserialize(using = LocalTimeDeserializer.class)
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @NotNull
    @Schema(example = "17:22")
    @JsonSerialize(using = LocalTimeSerializer.class)
    @JsonDeserialize(using = LocalTimeDeserializer.class)
    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

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

    @Schema(example = "특이사항")
    private String note;

    public static GetOrganizationReservationResDto toDto(OrganizationReservation organizationReservation){
        return GetOrganizationReservationResDto.builder()
                .organizationReservationId(organizationReservation.getId())
                .organizationName(organizationReservation.getOrganizationName())
                .reservationHolderId(organizationReservation.getUser().getId())
                .reservationHolder(organizationReservation.getName())
                .reservationPhoneNumber(organizationReservation.getPhoneNumber())
                .visitDate(organizationReservation.getVisitDate() != null? organizationReservation.getVisitDate() : null)
                .startTime(organizationReservation.getStartTime() != null? organizationReservation.getStartTime() : null)
                .endTime(organizationReservation.getEndTime() != null? organizationReservation.getEndTime().minusHours(1) : null)
                .address(organizationReservation.getAddress())
                .requirement(organizationReservation.getRequirement())
                .recipientGender(organizationReservation.getRecipientGender().getKorean())
                .recipientNumber(organizationReservation.getRecipientNumber())
                .reservationStatus(organizationReservation.getReservationStatus().getKorean())
                .note(organizationReservation.getNote() != null? organizationReservation.getNote() : "")
                .build();
    }

    public static List<GetOrganizationReservationResDto> toDtoList(List<OrganizationReservation> organizationReservations){
        if(organizationReservations.isEmpty()) {
            return new ArrayList<>();
        }
        return organizationReservations.stream().map(GetOrganizationReservationResDto::toDto).toList();


    }
}
