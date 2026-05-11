package com.project.ds_helper.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
@Getter
public class OrganizationLoginReqDto {

    @NotEmpty
    @Email(message = "이메일 형식에 맞지 않습니다.", regexp = "^[a-zA-Z0-9+-\\_.]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+$")
    @Schema(example = "test@test.com")
    private String email;

    @NotEmpty
    @Schema(example = "testtest")
    private String password;
}
