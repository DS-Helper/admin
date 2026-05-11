package com.project.ds_helper.domain.reservation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class DeletePersonalReservationReqDto {

    @NotBlank
    @Schema(example = "20949013-9cbe-44e0-9916-f4e919bcef64")
    private String personalReservationId;
}
