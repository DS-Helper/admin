package com.project.ds_helper.domain.user;

import com.project.ds_helper.domain.user.entity.User;
import com.project.ds_helper.domain.user.entity.GoogleOauth;
import com.project.ds_helper.domain.user.entity.KakaoOauth;
import com.project.ds_helper.domain.user.entity.NaverOauth;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class UserDtoEntityTest {

    @Test
    @DisplayName("User softDelete는 삭제 여부와 삭제일시를 저장한다")
    void userSoftDelete_savesDeletedFlagAndDeletedAt() {
        User user = User.builder().id("user-1").build();
        LocalDateTime deletedAt = LocalDateTime.of(2026, 5, 21, 14, 30);

        user.softDelete(deletedAt);

        assertThat(user.isDeleted()).isTrue();
        assertThat(user.getDeletedAt()).isEqualTo(deletedAt);
    }

    @Test
    @DisplayName("User recordLogin은 마지막 로그인 일시를 저장한다")
    void userRecordLogin_savesLastLoginAt() {
        User user = User.builder().id("user-1").build();
        LocalDateTime lastLoginAt = LocalDateTime.of(2026, 5, 22, 9, 10);

        user.recordLogin(lastLoginAt);

        assertThat(user.getLastLoginAt()).isEqualTo(lastLoginAt);
    }

    @Test
    @DisplayName("User 저장 전 UUID를 생성한다")
    void userPrePersist_generatesUuid() {
        User user = new User();

        ReflectionTestUtils.invokeMethod(user, "prePersistGenerateId");

        assertThat(user.getId()).isNotBlank();
    }

    @Test
    @DisplayName("KakaoOauth refreshToken은 값이 있을 때만 갱신한다")
    void kakaoOauthUpdateRefreshToken_updatesOnlyWhenValueExists() {
        KakaoOauth kakaoOauth = KakaoOauth.builder().refreshToken("old-token").build();

        kakaoOauth.updateRefreshToken("new-token");
        kakaoOauth.updateRefreshToken(" ");

        assertThat(kakaoOauth.getRefreshToken()).isEqualTo("new-token");
    }

    @Test
    @DisplayName("GoogleOauth refreshToken은 값이 있을 때만 갱신한다")
    void googleOauthUpdateRefreshToken_updatesOnlyWhenValueExists() {
        GoogleOauth googleOauth = GoogleOauth.builder().refreshToken("old-token").build();

        googleOauth.updateRefreshToken("new-token");
        googleOauth.updateRefreshToken(null);
        googleOauth.updateRefreshToken(" ");

        assertThat(googleOauth.getRefreshToken()).isEqualTo("new-token");
    }

    @Test
    @DisplayName("NaverOauth refreshToken은 값이 있을 때만 갱신한다")
    void naverOauthUpdateRefreshToken_updatesOnlyWhenValueExists() {
        NaverOauth naverOauth = NaverOauth.builder().refreshToken("old-token").build();

        naverOauth.updateRefreshToken("new-token");
        naverOauth.updateRefreshToken("");

        assertThat(naverOauth.getRefreshToken()).isEqualTo("new-token");
    }

    @Test
    @DisplayName("OAuth 엔티티 저장 전 UUID를 생성한다")
    void oauthPrePersist_generatesUuid() {
        KakaoOauth kakaoOauth = new KakaoOauth();
        GoogleOauth googleOauth = new GoogleOauth();
        NaverOauth naverOauth = new NaverOauth();

        ReflectionTestUtils.invokeMethod(kakaoOauth, "perPersistGenerateId");
        ReflectionTestUtils.invokeMethod(googleOauth, "perPersistGenerateId");
        ReflectionTestUtils.invokeMethod(naverOauth, "perPersistGenerateId");

        assertThat(kakaoOauth.getId()).isNotBlank();
        assertThat(googleOauth.getId()).isNotBlank();
        assertThat(naverOauth.getId()).isNotBlank();
    }
}
