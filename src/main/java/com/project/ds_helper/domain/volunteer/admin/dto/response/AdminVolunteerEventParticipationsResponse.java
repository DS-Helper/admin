package com.project.ds_helper.domain.volunteer.admin.dto.response;

import java.util.List;

public record AdminVolunteerEventParticipationsResponse(AdminVolunteerEventResponse event, List<AdminVolunteerParticipationResponse> participations) { }
