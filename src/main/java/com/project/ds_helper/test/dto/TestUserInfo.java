package com.project.ds_helper.test.dto;

import com.project.ds_helper.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "테스트용 사용자 정보 DTO", example = "{\"userId\":\"user-001\",\"email\":\"user@test.com\",\"role\":\"USER\",\"type\":\"NORMAL\",\"profileImageUrl\":\"https://cdn.dshelper.kr/profile/user-001.png\",\"gender\":\"남성\",\"ageRange\":\"20~29\",\"birthday\":\"03-23\",\"birthyear\":\"1998\"}")
public record TestUserInfo(
        String userId,
        String email,
        String role,
        String type,
        String profileImageUrl,
        String gender,
        String ageRange,
        String birthday,
        String birthyear
) {
    public static TestUserInfo toDto(User user){
        return TestUserInfo.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .type(user.getType().name())
                .profileImageUrl(user.getProfileImageUrl())
                .gender(user.getGender())
                .ageRange(user.getAgeRange())
                .birthday(user.getBirthday())
                .birthyear(user.getBirthyear())
                .build();
    }
}
