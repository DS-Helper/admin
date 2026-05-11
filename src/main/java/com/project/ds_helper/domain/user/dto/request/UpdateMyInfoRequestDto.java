package com.project.ds_helper.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(
        description = "내 정보 수정 요청 DTO",
        example = "{\"name\":\"홍길동\",\"email\":\"user@test.com\",\"birthyear\":\"1998\",\"gender\":\"male\",\"phoneNumber\":\"010-1234-5678\",\"removeProfileImage\":false}"
)
public class UpdateMyInfoRequestDto {

    @Schema(description = "이름", example = "홍길동")
    private String name;

    @Schema(description = "이메일", example = "user@test.com")
    private String email;

    @Schema(description = "출생 연도", example = "1998")
    private String birthyear;

    @Schema(description = "성별", example = "male")
    private String gender;

    @Schema(description = "전화번호", example = "010-1234-5678")
    private String phoneNumber;

    @Schema(description = "기존 프로필 이미지 제거 여부", example = "false")
    private boolean removeProfileImage;
}
