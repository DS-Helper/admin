package com.project.ds_helper.domain.user.dto.response;

import com.project.ds_helper.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "사용자 정보 DTO", example = "{\"userId\":\"user-001\",\"email\":\"user@test.com\",\"role\":\"USER\",\"name\":\"홍길동\",\"gender\":\"남성\"}")
public class UserDto {

    private String userId;

    private String email;

    private String role;

    private String name;

    private String gender;

    public static UserDto toDto(User user){
        return UserDto.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .role(user.getRole().name())
                .name(user.getName())
                .gender(user.getGender())
                .build();
    }
}
