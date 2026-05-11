package com.project.ds_helper.domain.reservation.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalTime;
import java.util.List;

public record GetPreReservedReservationByDateResDto(
//        @Schema(example = "HH:mm")
//        @JsonSerialize(using = LocalTimeSerializer.class)
//        @JsonDeserialize(using = LocalTimeDeserializer.class)
//        @JsonFormat(pattern = "HH:mm")
//        LocalTime startTime,
//        @Schema(example = "HH:mm")
//        @JsonSerialize(using = LocalTimeSerializer.class)
//        @JsonDeserialize(using = LocalTimeDeserializer.class)
//        @JsonFormat(pattern = "HH:mm")
//        LocalTime endTime

        List<String> preReservedSchedules
) {
}
