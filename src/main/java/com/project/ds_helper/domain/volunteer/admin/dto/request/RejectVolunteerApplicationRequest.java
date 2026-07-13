package com.project.ds_helper.domain.volunteer.admin.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RejectVolunteerApplicationRequest(
        @NotBlank String rejectionReason,
        String adminMemo
) {
}
