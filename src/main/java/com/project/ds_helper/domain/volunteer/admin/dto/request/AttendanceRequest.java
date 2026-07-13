package com.project.ds_helper.domain.volunteer.admin.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AttendanceRequest(@NotNull List<String> attendedParticipationIds, @NotNull List<String> absentParticipationIds) {
}
