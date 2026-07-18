package com.project.ds_helper.domain.volunteer.admin.dto.response;

import com.project.ds_helper.domain.volunteer.common.enums.VolunteerParticipationStatus;
import com.project.ds_helper.domain.volunteer.participation.entity.VolunteerParticipation;

import java.time.Instant;

public record AdminVolunteerParticipationResponse(String participationId, String memberId, String name, String phone, VolunteerParticipationStatus participationStatus, Instant appliedAt, Instant attendanceCheckedAt) {
    public static AdminVolunteerParticipationResponse from(VolunteerParticipation participation) {
        return new AdminVolunteerParticipationResponse(participation.getId(), participation.getMember().getId(), participation.getMember().getUser().getName(), maskPhone(participation.getMember().getApplication().getPhone()), participation.getStatus(), participation.getAppliedAt(), participation.getAttendanceCheckedAt());
    }
    private static String maskPhone(String phone) { return phone != null && phone.length() >= 4 ? phone.substring(0, Math.min(3, phone.length())) + "-****-" + phone.substring(phone.length() - 4) : "***"; }
}
