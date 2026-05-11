package com.project.ds_helper.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.mail.Message;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
@Getter
public class OrganizationJoinReqDto {

    @NotEmpty
    @Email(message = "이메일 형식에 맞지 않습니다.", regexp = "^[a-zA-Z0-9+-\\_.]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+$")
    @Schema(example = "test@test.com")
    private String email;

    @NotEmpty
    @Schema(example = "testtest")
    private String password;

    @NotEmpty
    @Schema(example = "testtest")
    private String passwordCheck;

    @NotEmpty
    @Schema(example = "testOrganization")
    private String organizationName;

    @NotEmpty
    @Pattern(
            regexp = "^01[0-9]-\\d{3,4}-\\d{4}$",
            message = "올바른 휴대폰 번호 형식이 아닙니다."
    )
    @Schema(example = "010-0000-0000")
    private String organizationPhoneNumber;
}
