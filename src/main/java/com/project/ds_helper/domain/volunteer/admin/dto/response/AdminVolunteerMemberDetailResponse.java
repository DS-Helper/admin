package com.project.ds_helper.domain.volunteer.admin.dto.response;

import com.project.ds_helper.domain.volunteer.common.enums.VolunteerParticipationStatus;
import com.project.ds_helper.domain.volunteer.history.entity.VolunteerStatusHistory;
import com.project.ds_helper.domain.volunteer.member.entity.VolunteerMember;
import com.project.ds_helper.domain.volunteer.participation.entity.VolunteerParticipation;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

public record AdminVolunteerMemberDetailResponse(AdminVolunteerMemberResponse member, AdminVolunteerApplicationResponse application, long totalParticipationCount, long totalHours, List<ParticipationHistory> upcomingEvents, List<ParticipationHistory> attendanceHistory, List<ParticipationHistory> absenceHistory, List<ParticipationHistory> cancellationHistory, String adminMemo, List<StatusHistory> statusHistory) {
    public static AdminVolunteerMemberDetailResponse from(VolunteerMember member, List<VolunteerParticipation> participations, List<VolunteerStatusHistory> histories, Instant now) {
        List<ParticipationHistory> mapped = participations.stream().map(ParticipationHistory::from).toList();
        List<ParticipationHistory> upcoming = participations.stream().filter(p -> p.getStatus() == VolunteerParticipationStatus.APPLIED && p.getEvent().getStartAt().isAfter(now)).map(ParticipationHistory::from).toList();
        long attended = participations.stream().filter(p -> p.getStatus() == VolunteerParticipationStatus.ATTENDED).count();
        long hours = participations.stream().filter(p -> p.getStatus() == VolunteerParticipationStatus.ATTENDED).mapToLong(p -> Duration.between(p.getEvent().getStartAt(), p.getEvent().getEndAt()).toHours()).sum();
        return new AdminVolunteerMemberDetailResponse(AdminVolunteerMemberResponse.from(member), AdminVolunteerApplicationResponse.from(member.getApplication()), attended, hours, upcoming, mapped.stream().filter(p -> p.status() == VolunteerParticipationStatus.ATTENDED).toList(), mapped.stream().filter(p -> p.status() == VolunteerParticipationStatus.ABSENT).toList(), mapped.stream().filter(p -> p.status() == VolunteerParticipationStatus.CANCELED).toList(), member.getApplication().getAdminMemo(), histories.stream().map(StatusHistory::from).toList());
    }
    public record ParticipationHistory(String participationId, String eventId, String title, Instant startAt, Instant endAt, String location, VolunteerParticipationStatus status, Long recognizedHours) { static ParticipationHistory from(VolunteerParticipation p) { return new ParticipationHistory(p.getId(), p.getEvent().getId(), p.getEvent().getTitle(), p.getEvent().getStartAt(), p.getEvent().getEndAt(), p.getEvent().getLocation(), p.getStatus(), p.getStatus() == VolunteerParticipationStatus.ATTENDED ? Duration.between(p.getEvent().getStartAt(), p.getEvent().getEndAt()).toHours() : null); } }
    public record StatusHistory(String id, String previousStatus, String nextStatus, String reason, String changedBy, java.time.LocalDateTime changedAt) { static StatusHistory from(VolunteerStatusHistory h) { return new StatusHistory(h.getId(), h.getPreviousStatus(), h.getNextStatus(), h.getChangeReason(), h.getChangedBy(), h.getCreatedAt()); } }
}
