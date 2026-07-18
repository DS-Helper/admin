package com.project.ds_helper.domain.volunteer.admin.dto.response;

import java.time.Instant;

public record VolunteerAttendanceResponse(String eventId, int attendedCount, int absentCount, Instant processedAt) { }
