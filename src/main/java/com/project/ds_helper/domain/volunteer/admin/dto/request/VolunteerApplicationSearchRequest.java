package com.project.ds_helper.domain.volunteer.admin.dto.request;

import com.project.ds_helper.domain.volunteer.common.enums.VolunteerApplicationStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record VolunteerApplicationSearchRequest(
        String keyword, String name, String phone, VolunteerApplicationStatus status,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate appliedFrom,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate appliedTo,
        Integer page, Integer size
) { }
