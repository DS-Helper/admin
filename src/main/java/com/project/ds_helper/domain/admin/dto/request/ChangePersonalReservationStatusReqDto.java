package com.project.ds_helper.domain.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public record ChangePersonalReservationStatusReqDto(
        @Schema(example = "3c623822-56f4-4fc3-97e2-7c273174173c")
        @NotBlank
        String personalReservationId,

        @Schema(example = "완료")
        @NotBlank
        String status
) {
        public void logFields(){
                log.info("personalReservationId : {}, status, {}", personalReservationId, status );
        }
}
