package com.project.ds_helper.domain.user.entity;

import com.project.ds_helper.domain.base.entity.BaseTime;
import com.project.ds_helper.domain.user.enums.UserRole;
import com.project.ds_helper.domain.user.enums.UserType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_user")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class User extends BaseTime {

    @PrePersist
    private void prePersistGenerateId(){
        this.id = String.valueOf(UUID.randomUUID());
    }

    @Id
    @Column(name = "user_id")
    private String id; // 식별자

    @Column(name = "email", unique = true)
    private String email; // 이메일

    @Column(name = "password")
    private String password; // 비밀번호

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "role")
    private UserRole role = UserRole.USER; // 권한

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private UserType type = UserType.PERSONAL; // 권한

    @Column(name = "name")
    private String name; // 이름

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @Column(name = "gender")
    private String gender;

    @Column(name = "age_range")
    private String ageRange; // 삭제 예정

    @Column(name = "birthday") // 삭제 예정
    private String birthday;

    @Column(name = "birthyear")
    private String birthyear;

    @Builder.Default
    @Column(name = "phone_number")
    private String phoneNumber = "010-0000-0000";

    @Builder.Default
    @Column(name = "local_auth_connected")
    private boolean localAuthConnected = false; // 자체 회원가입 여부

    @Builder.Default
    @Column(name = "kakao_oauth_connected")
    private boolean kakaoOauthConnected = false; // 카카오 소셜 회원가입 여부

    @Builder.Default
    @Column(name = "google_oauth_connected")
    private boolean googleOauthConnected = false; // 구글 소셜 회원가입 여부

    @Builder.Default
    @Column(name = "naver_oauth_connected")
    private boolean naverOauthConnected = false; // 네이버 소셜 회원가입 여부

    @Builder.Default
    @Column(name = "is_deleted")
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "last_login_at", nullable = true)
    private LocalDateTime lastLoginAt;

    public void softDelete(LocalDateTime deletedAt) {
        this.isDeleted = true;
        this.deletedAt = deletedAt;
    }

    public void recordLogin(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
}
