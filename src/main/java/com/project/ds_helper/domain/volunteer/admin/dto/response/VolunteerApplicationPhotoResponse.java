package com.project.ds_helper.domain.volunteer.admin.dto.response;

import java.time.Instant;

public record VolunteerApplicationPhotoResponse(String fileId, String url, Instant expiresAt) { }
