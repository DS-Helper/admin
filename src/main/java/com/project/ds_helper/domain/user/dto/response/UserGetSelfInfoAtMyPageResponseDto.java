package com.project.ds_helper.domain.user.dto.response;

import com.project.ds_helper.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "마이페이지 내 정보 응답 DTO", example = "{\"name\":\"홍길동\",\"email\":\"user@test.com\",\"birthyear\":\"1998\",\"gender\":\"남성\",\"phoneNumber\":\"010-1234-5678\",\"profileImageUrl\":\"https://cdn.dshelper.kr/profile/user-001.png\"}")
public record UserGetSelfInfoAtMyPageResponseDto(
        // 이름, 이메일, 연령대, 성별, 전화번호, 프로필 사진 경로
        String name,
        String email,
        String birthyear,
        String gender,
        String phoneNumber,
        String profileImageUrl
) {
    public static UserGetSelfInfoAtMyPageResponseDto toDto(User user){

        String name = user.getName() == null? null : user.getName();
        String email = user.getEmail() == null? null : user.getEmail();
        String birthyear = user.getBirthyear() == null? null : user.getBirthyear();
        String gender = user.getGender() == null? null : user.getGender();
        String phoneNumber = user.getPhoneNumber() == null? null : user.getPhoneNumber();
        String profileImageUrl = user.getProfileImageUrl() == null? null : user.getProfileImageUrl();

        return new UserGetSelfInfoAtMyPageResponseDto(name, email, birthyear, gender, phoneNumber, profileImageUrl);
    }
}
