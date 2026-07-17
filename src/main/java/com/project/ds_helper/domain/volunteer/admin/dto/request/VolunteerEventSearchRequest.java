package com.project.ds_helper.domain.volunteer.admin.dto.request;

import com.project.ds_helper.domain.volunteer.common.enums.VolunteerEventStatus;
import com.project.ds_helper.domain.volunteer.common.enums.VolunteerEventVisibility;

public record VolunteerEventSearchRequest(String keyword, VolunteerEventStatus status, VolunteerEventVisibility visibility, Integer page, Integer size) { }
