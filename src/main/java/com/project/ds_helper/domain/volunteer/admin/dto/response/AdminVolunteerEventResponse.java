package com.project.ds_helper.domain.volunteer.admin.dto.response;

import com.project.ds_helper.domain.volunteer.common.enums.VolunteerEventCloseReason;
import com.project.ds_helper.domain.volunteer.common.enums.VolunteerEventStatus;
import com.project.ds_helper.domain.volunteer.common.enums.VolunteerEventVisibility;
import com.project.ds_helper.domain.volunteer.event.entity.VolunteerEvent;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public record AdminVolunteerEventResponse(
        String id, String title, String type, String typeLabel, String imageFileId, String imageUrl, Instant startAt, Instant endAt,
        Instant recruitmentDeadlineAt, String location, int capacity, long participantCount, String description,
        List<String> supplies, List<String> precautions, VolunteerEventStatus status, VolunteerEventCloseReason closeReason,
        VolunteerEventVisibility visibility, String cancelReason, String createdBy, String updatedBy, LocalDateTime createdAt, LocalDateTime updatedAt,
        AdminVolunteerEventCapabilities capabilities
) {
    public static AdminVolunteerEventResponse from(VolunteerEvent event, String imageUrl, long participantCount) {
        return new AdminVolunteerEventResponse(event.getId(), event.getTitle(), event.getType(), event.getType(), event.getImageFile().getId(), imageUrl,
                event.getStartAt(), event.getEndAt(), event.getRecruitmentDeadlineAt(), event.getLocation(), event.getCapacity(),
                participantCount, event.getDescription(), lines(event.getSupplies()), lines(event.getPrecautions()), event.getStatus(),
                event.getCloseReason(), event.getVisibility(), event.getCancelReason(), event.getCreatedBy(), event.getUpdatedBy(), event.getCreatedAt(), event.getUpdatedAt(),
                new AdminVolunteerEventCapabilities(event.getStatus() != VolunteerEventStatus.COMPLETED && event.getStatus() != VolunteerEventStatus.CANCELED, event.getStatus() == VolunteerEventStatus.DRAFT || event.getStatus() == VolunteerEventStatus.CLOSED, event.getStatus() == VolunteerEventStatus.OPEN, event.getStatus() == VolunteerEventStatus.OPEN || event.getStatus() == VolunteerEventStatus.CLOSED, event.getStatus() == VolunteerEventStatus.COMPLETED));
    }

    private static List<String> lines(String value) { return value == null || value.isBlank() ? List.of() : Arrays.stream(value.split("\\R")).map(String::trim).filter(line -> !line.isEmpty()).toList(); }

    public record AdminVolunteerEventCapabilities(boolean canEdit, boolean canOpen, boolean canClose, boolean canCancel, boolean canManageAttendance) { }
}
