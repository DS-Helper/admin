package com.project.ds_helper.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "관리자 로그인 요청 DTO")
public class AdminLoginReqDto {

    @Schema(description = "사용자명(이메일)", example = "admin@dshelper.kr")
    private String username;

    @Schema(description = "비밀번호", example = "admin1234")
    private String password;
}
