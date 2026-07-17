package com.project.ds_helper.domain.volunteer.admin.dto.request;

import com.project.ds_helper.domain.volunteer.common.enums.VolunteerMemberStatus;

public record VolunteerMemberSearchRequest(String keyword, VolunteerMemberStatus status, String gender, Integer page, Integer size) { }
