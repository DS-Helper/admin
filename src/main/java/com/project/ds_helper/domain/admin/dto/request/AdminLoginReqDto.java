package com.project.ds_helper.domain.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "관리자 로그인 요청 DTO")
public class AdminLoginReqDto {

    @NotBlank(message = "아이디를 입력해주세요.")
    @Schema(description = "관리자 아이디", example = "admin")
    private String username;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Schema(description = "관리자 비밀번호", example = "admin1234")
    private String password;
}
