package com.project.ds_helper.domain.volunteer.admin.dto.response;

import com.project.ds_helper.domain.volunteer.application.entity.VolunteerApplication;
import com.project.ds_helper.domain.volunteer.common.enums.VolunteerApplicationStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AdminVolunteerApplicationResponse(
        String id, String userId, String name, String phone, LocalDate birthDate, String gender,
        String neighborhood, List<String> preferredActivities, String motivation,
        VolunteerApplicationStatus status, String rejectionReason, String adminMemo,
        String reviewedBy, Instant reviewedAt, LocalDateTime createdAt, LocalDateTime updatedAt,
        AdminVolunteerApplicationCapabilities capabilities
) {
    public static AdminVolunteerApplicationResponse from(VolunteerApplication application) {
        return new AdminVolunteerApplicationResponse(application.getId(), application.getUser().getId(), application.getName(),
                application.getPhone(), application.getBirthDate(), application.getGender(), application.getNeighborhood(),
                List.copyOf(application.getPreferredActivities()), application.getMotivation(), application.getStatus(),
                application.getRejectionReason(), application.getAdminMemo(), application.getReviewedBy(), application.getReviewedAt(), application.getCreatedAt(), application.getUpdatedAt(),
                new AdminVolunteerApplicationCapabilities(application.getStatus() == VolunteerApplicationStatus.PENDING, application.getStatus() == VolunteerApplicationStatus.PENDING));
    }

    public record AdminVolunteerApplicationCapabilities(boolean canApprove, boolean canReject) { }
}
