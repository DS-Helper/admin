package com.project.ds_helper.domain.volunteer.admin.dto.response;

import com.project.ds_helper.domain.volunteer.common.enums.VolunteerMemberStatus;
import com.project.ds_helper.domain.volunteer.member.entity.VolunteerMember;

import java.time.Instant;

public record AdminVolunteerMemberResponse(String id, String userId, String name, String gender, String phone, VolunteerMemberStatus status, Instant joinedAt, long totalParticipationCount, long totalHours, AdminVolunteerMemberCapabilities capabilities) {
    public static AdminVolunteerMemberResponse from(VolunteerMember member) {
        return from(member, 0, 0);
    }
    public static AdminVolunteerMemberResponse from(VolunteerMember member, long totalParticipationCount, long totalHours) { return new AdminVolunteerMemberResponse(member.getId(), member.getUser().getId(), member.getUser().getName(), member.getApplication().getGender(), member.getApplication().getPhone(), member.getStatus(), member.getJoinedAt(), totalParticipationCount, totalHours, new AdminVolunteerMemberCapabilities(member.getStatus() == VolunteerMemberStatus.SUSPENDED, member.getStatus() == VolunteerMemberStatus.ACTIVE, member.getStatus() != VolunteerMemberStatus.WITHDRAWN)); }

    public record AdminVolunteerMemberCapabilities(boolean canActivate, boolean canSuspend, boolean canWithdraw) { }
}
