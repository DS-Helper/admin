package com.project.ds_helper.domain.volunteer.admin.dto.request;

import com.project.ds_helper.domain.volunteer.common.enums.VolunteerEventStatus;
import com.project.ds_helper.domain.volunteer.common.enums.VolunteerEventVisibility;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CreateVolunteerEventRequest(
        @NotBlank String title, @NotBlank String type, @NotBlank String imageFileId,
        @NotNull Instant startAt, @NotNull Instant endAt, @NotNull Instant recruitmentDeadlineAt,
        @NotBlank String location, @NotNull @Min(1) Integer capacity, @NotBlank String description,
        String supplies, String precautions, @NotNull VolunteerEventStatus status,
        @NotNull VolunteerEventVisibility visibility
) {
}
