package com.project.ds_helper.domain.user.service;

import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.post.util.ImageCompressionUtil;
import com.project.ds_helper.domain.post.util.ImageUtil;
import com.project.ds_helper.domain.post.util.S3Util;
import com.project.ds_helper.domain.user.dto.request.UpdateMyInfoRequestDto;
import com.project.ds_helper.domain.user.dto.response.UserGetSelfInfoAtMyPageResponseDto;
import com.project.ds_helper.domain.user.entity.User;
import org.apache.coyote.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock private UserUtil userUtil;
    @Mock private S3Util s3Util;
    @Mock private ImageCompressionUtil imageCompressionUtil;
    @Mock private ImageUtil imageUtil;
    @Mock private Authentication authentication;
    @Mock private MultipartFile profileImage;

    @InjectMocks
    private UserProfileService userProfileService;

    @Test
    @DisplayName("내 정보 조회는 사용자 DTO를 반환한다")
    void getMyInfo_returnsDto() {
        User user = User.builder().id("user-1").email("user@test.com").name("홍길동").build();
        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(userUtil.findUserById("user-1")).thenReturn(user);

        UserGetSelfInfoAtMyPageResponseDto result = userProfileService.getMyInfo(authentication);

        assertThat(result.email()).isEqualTo("user@test.com");
        assertThat(result.name()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("프로필 수정은 기존 이미지를 유지한다")
    void updateMyInfo_keepsExistingProfileImage() throws Exception {
        User user = User.builder().id("user-1").email("user@test.com").profileImageUrl("https://bucket.s3/current.png").build();
        UpdateMyInfoRequestDto dto = new UpdateMyInfoRequestDto();
        ReflectionTestUtils.setField(dto, "email", "user@test.com");
        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(userUtil.findUserById("user-1")).thenReturn(user);

        UserGetSelfInfoAtMyPageResponseDto result = userProfileService.updateMyInfo(authentication, dto, null);

        assertThat(result.profileImageUrl()).isEqualTo("https://bucket.s3/current.png");
    }

    @Test
    @DisplayName("프로필 수정은 새 이미지를 교체한다")
    void updateMyInfo_replacesProfileImage() throws Exception {
        User user = User.builder().id("user-1").email("user@test.com").profileImageUrl("https://bucket.s3/current.png").build();
        UpdateMyInfoRequestDto dto = new UpdateMyInfoRequestDto();
        ReflectionTestUtils.setField(dto, "email", "user@test.com");
        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(userUtil.findUserById("user-1")).thenReturn(user);
        when(profileImage.isEmpty()).thenReturn(false);
        when(imageUtil.toStoredFilename()).thenReturn("new.png");
        when(imageCompressionUtil.compressImage(any(), any())).thenReturn(
                com.project.ds_helper.common.dto.request.S3ImageUploadRequestDto.builder()
                        .storedFilename("new.png").originalFilename("new.png").bytes(new byte[]{1}).size(1L)
                        .s3ContentType("image/png").fileExtension("png").build()
        );
        when(s3Util.toS3UrlByStoredFilename("new.png")).thenReturn("https://bucket.s3/new.png");

        UserGetSelfInfoAtMyPageResponseDto result = userProfileService.updateMyInfo(authentication, dto, profileImage);

        assertThat(result.profileImageUrl()).isEqualTo("https://bucket.s3/new.png");
    }

    @Test
    @DisplayName("프로필 수정은 삭제와 업로드를 동시에 허용하지 않는다")
    void updateMyInfo_throwsWhenRemoveAndUploadRequestedTogether() {
        User user = User.builder().id("user-1").email("user@test.com").build();
        UpdateMyInfoRequestDto dto = new UpdateMyInfoRequestDto();
        ReflectionTestUtils.setField(dto, "removeProfileImage", true);
        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(userUtil.findUserById("user-1")).thenReturn(user);
        when(profileImage.isEmpty()).thenReturn(false);

        assertThatThrownBy(() -> userProfileService.updateMyInfo(authentication, dto, profileImage))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("프로필 수정은 이메일이 중복되면 예외가 발생한다")
    void updateMyInfo_throwsWhenEmailExists() {
        User user = User.builder().id("user-1").email("user@test.com").build();
        UpdateMyInfoRequestDto dto = new UpdateMyInfoRequestDto();
        ReflectionTestUtils.setField(dto, "email", "other@test.com");
        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(userUtil.findUserById("user-1")).thenReturn(user);
        when(userUtil.existsByEmail("other@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userProfileService.updateMyInfo(authentication, dto, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email Already Exist");
    }

    @Test
    @DisplayName("프로필 수정은 관리 이미지가 아니면 삭제 호출 없이 null로 처리한다")
    void updateMyInfo_doesNotDeleteUnmanagedImage() throws Exception {
        User user = User.builder().id("user-1").email("user@test.com").profileImageUrl("https://external.example.com/current.png").build();
        UpdateMyInfoRequestDto dto = new UpdateMyInfoRequestDto();
        ReflectionTestUtils.setField(dto, "removeProfileImage", true);
        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(userUtil.findUserById("user-1")).thenReturn(user);
        when(s3Util.isManagedS3Url("https://external.example.com/current.png")).thenReturn(false);

        UserGetSelfInfoAtMyPageResponseDto result = userProfileService.updateMyInfo(authentication, dto, null);

        assertThat(result.profileImageUrl()).isNull();
    }
}
