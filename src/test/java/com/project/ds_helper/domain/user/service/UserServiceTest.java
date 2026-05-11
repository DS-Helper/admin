package com.project.ds_helper.domain.user.service;

import com.project.ds_helper.common.enums.JwtTokenType;
import com.project.ds_helper.common.util.CookieUtil;
import com.project.ds_helper.common.util.JwtUtil;
import com.project.ds_helper.common.util.PasswordUtil;
import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.post.util.ImageCompressionUtil;
import com.project.ds_helper.domain.post.util.ImageUtil;
import com.project.ds_helper.domain.post.util.S3Util;
import com.project.ds_helper.domain.user.dto.request.OrganizationJoinReqDto;
import com.project.ds_helper.domain.user.dto.request.UpdateMyInfoRequestDto;
import com.project.ds_helper.domain.user.dto.request.UserJoinReqDto;
import com.project.ds_helper.domain.user.dto.response.UserIdentifierResponseDto;
import com.project.ds_helper.domain.user.dto.response.UserGetSelfInfoAtMyPageResponseDto;
import com.project.ds_helper.domain.user.entity.User;
import com.project.ds_helper.domain.user.enums.UserType;
import com.project.ds_helper.domain.user.repository.OrganizationRepository;
import com.project.ds_helper.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private PasswordUtil passwordUtil;

    @Mock
    private S3Util s3Util;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private CookieUtil cookieUtil;

    @Mock
    private ImageCompressionUtil imageCompressionUtil;

    @Mock
    private ImageUtil imageUtil;

    @Mock
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private UserUtil userUtil;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Authentication authentication;

    @Mock
    private MultipartFile profileImage;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("로그인 체크는 refresh token이 비어 있으면 false를 반환한다")
    void checkIfUserLoggedIn_returnsFalseWhenRefreshTokenBlank() {
        Object result = userService.checkIfUserLoggedIn("");

        assertThat(result).isEqualTo(false);
    }

    @Test
    @DisplayName("로그인 체크는 redis refresh token이 유효하면 true를 반환한다")
    void checkIfUserLoggedIn_returnsTrueWhenRedisTokenValid() {
        when(jwtUtil.isExpired("refresh-token")).thenReturn(false);
        when(jwtUtil.getId("refresh-token")).thenReturn("user-1");
        when(jwtUtil.toRedisRefreshTokenKey("user-1")).thenReturn("refresh:user-1");
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("refresh:user-1")).thenReturn("refresh-token");

        Object result = userService.checkIfUserLoggedIn("refresh-token");

        assertThat(result).isEqualTo(true);
    }

    @Test
    @DisplayName("JWT 생성 후 redis와 헤더에 토큰을 저장한다")
    void generateJwtTokenAndPutInCookie_setsRedisAndCookies() {
        when(jwtUtil.generateAccessToken("user-1", "USER", "PERSONAL")).thenReturn("access");
        when(jwtUtil.generateRefreshToken("user-1", "USER", "PERSONAL")).thenReturn("refresh");
        when(jwtUtil.toRedisRefreshTokenKey("user-1")).thenReturn("refresh:user-1");
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        userService.generateJwtTokenAndPutInResponseHeader(response, "user-1", "USER", "PERSONAL");

        verify(valueOperations).set("refresh:user-1", "refresh");
        verify(response).setHeader(JwtTokenType.ACCESS_TOKEN_NAME.getTokenName(), "access");
        verify(response).setHeader(JwtTokenType.REFRESH_TOKEN_NAME.getTokenName(), "refresh");
    }

    @Test
    @DisplayName("일반 회원 가입은 이메일이 중복이면 예외가 발생한다")
    void userJoin_throwsWhenEmailExists() {
        UserJoinReqDto dto = UserJoinReqDto.builder()
                .email("user@test.com")
                .password("pw")
                .passwordCheck("pw")
                .build();
        when(userUtil.existsByEmail("user@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.userJoin(dto, response))
                .isInstanceOf(org.apache.coyote.BadRequestException.class)
                .hasMessageContaining("Email Already Exist");
    }

    @Test
    @DisplayName("기관 회원 가입은 비밀번호가 다르면 예외가 발생한다")
    void organizationJoin_throwsWhenPasswordMismatch() {
        OrganizationJoinReqDto dto = OrganizationJoinReqDto.builder()
                .email("org@test.com")
                .password("pw1")
                .passwordCheck("pw2")
                .organizationName("기관")
                .organizationPhoneNumber("010-1234-5678")
                .build();
        when(passwordUtil.isPasswordMatch("pw1", "pw2")).thenReturn(false);

        assertThatThrownBy(() -> userService.organizationJoin(dto, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Password Not Match");
    }

    @Test
    @DisplayName("내 정보 조회는 사용자 DTO를 반환한다")
    void getMyInfo_returnsDto() {
        User user = User.builder()
                .id("user-1")
                .email("user@test.com")
                .name("홍길동")
                .type(UserType.PERSONAL)
                .build();
        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(userUtil.findUserById("user-1")).thenReturn(user);

        UserGetSelfInfoAtMyPageResponseDto result = userService.getMyInfo(authentication);

        assertThat(result.email()).isEqualTo("user@test.com");
        assertThat(result.name()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("새 프로필 이미지가 없고 삭제 요청도 아니면 기존 프로필 이미지를 유지한다")
    void updateMyInfo_keepsExistingProfileImage() throws Exception {
        User user = User.builder()
                .id("user-1")
                .email("user@test.com")
                .name("홍길동")
                .profileImageUrl("https://bucket.s3.ap-northeast-2.amazonaws.com/images/current.png")
                .build();
        UpdateMyInfoRequestDto dto = new UpdateMyInfoRequestDto();
        ReflectionTestUtils.setField(dto, "name", "홍길동");
        ReflectionTestUtils.setField(dto, "email", "user@test.com");
        ReflectionTestUtils.setField(dto, "birthyear", "1998");
        ReflectionTestUtils.setField(dto, "gender", "male");
        ReflectionTestUtils.setField(dto, "phoneNumber", "010-1234-5678");
        ReflectionTestUtils.setField(dto, "removeProfileImage", false);

        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(userUtil.findUserById("user-1")).thenReturn(user);

        UserGetSelfInfoAtMyPageResponseDto result = userService.updateMyInfo(authentication, dto, null);

        assertThat(result.profileImageUrl()).isEqualTo("https://bucket.s3.ap-northeast-2.amazonaws.com/images/current.png");
        verify(s3Util, never()).deleteImagesByS3Key(any());
    }

    @Test
    @DisplayName("새 프로필 이미지가 있으면 기존 관리 이미지 삭제 후 새 이미지로 교체한다")
    void updateMyInfo_replacesProfileImage() throws Exception {
        User user = User.builder()
                .id("user-1")
                .email("user@test.com")
                .name("홍길동")
                .profileImageUrl("https://bucket.s3.ap-northeast-2.amazonaws.com/images/current.png")
                .build();
        UpdateMyInfoRequestDto dto = new UpdateMyInfoRequestDto();
        ReflectionTestUtils.setField(dto, "name", "홍길동");
        ReflectionTestUtils.setField(dto, "email", "user@test.com");
        ReflectionTestUtils.setField(dto, "birthyear", "1998");
        ReflectionTestUtils.setField(dto, "gender", "male");
        ReflectionTestUtils.setField(dto, "phoneNumber", "010-1234-5678");
        ReflectionTestUtils.setField(dto, "removeProfileImage", false);

        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(userUtil.findUserById("user-1")).thenReturn(user);
        when(profileImage.isEmpty()).thenReturn(false);
        when(imageUtil.toStoredFilename()).thenReturn("new-profile.png");
        when(imageCompressionUtil.compressImage(profileImage, "new-profile.png"))
                .thenReturn(compressedImage("new-profile.png", "profile.png", "png", "profile".getBytes()));
        when(s3Util.toS3UrlByStoredFilename("new-profile.png"))
                .thenReturn("https://bucket.s3.ap-northeast-2.amazonaws.com/images/new-profile.png");
        when(s3Util.isManagedS3Url("https://bucket.s3.ap-northeast-2.amazonaws.com/images/current.png")).thenReturn(true);
        when(s3Util.extractS3KeyFromS3Url("https://bucket.s3.ap-northeast-2.amazonaws.com/images/current.png"))
                .thenReturn("images/current.png");

        UserGetSelfInfoAtMyPageResponseDto result = userService.updateMyInfo(authentication, dto, profileImage);

        assertThat(result.profileImageUrl()).isEqualTo("https://bucket.s3.ap-northeast-2.amazonaws.com/images/new-profile.png");
        verify(s3Util).uploadImage(any());
        verify(s3Util).deleteImagesByS3Key(java.util.List.of("images/current.png"));
    }

    @Test
    @DisplayName("프로필 이미지 삭제 요청이면 기존 관리 이미지를 삭제하고 null로 변경한다")
    void updateMyInfo_removesProfileImage() throws Exception {
        User user = User.builder()
                .id("user-1")
                .email("user@test.com")
                .name("홍길동")
                .profileImageUrl("https://bucket.s3.ap-northeast-2.amazonaws.com/images/current.png")
                .build();
        UpdateMyInfoRequestDto dto = new UpdateMyInfoRequestDto();
        ReflectionTestUtils.setField(dto, "name", "홍길동");
        ReflectionTestUtils.setField(dto, "email", "user@test.com");
        ReflectionTestUtils.setField(dto, "birthyear", "1998");
        ReflectionTestUtils.setField(dto, "gender", "male");
        ReflectionTestUtils.setField(dto, "phoneNumber", "010-1234-5678");
        setRemoveProfileImage(dto, true);

        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(userUtil.findUserById("user-1")).thenReturn(user);
        when(s3Util.isManagedS3Url("https://bucket.s3.ap-northeast-2.amazonaws.com/images/current.png")).thenReturn(true);
        when(s3Util.extractS3KeyFromS3Url("https://bucket.s3.ap-northeast-2.amazonaws.com/images/current.png"))
                .thenReturn("images/current.png");

        UserGetSelfInfoAtMyPageResponseDto result = userService.updateMyInfo(authentication, dto, null);

        assertThat(result.profileImageUrl()).isNull();
        verify(s3Util).deleteImagesByS3Key(java.util.List.of("images/current.png"));
    }

    @Test
    @DisplayName("프로필 이미지 삭제와 새 파일 업로드를 동시에 요청하면 예외가 발생한다")
    void updateMyInfo_throwsWhenRemoveAndUploadRequestedTogether() {
        User user = User.builder()
                .id("user-1")
                .email("user@test.com")
                .name("홍길동")
                .build();
        UpdateMyInfoRequestDto dto = new UpdateMyInfoRequestDto();
        ReflectionTestUtils.setField(dto, "email", "user@test.com");
        setRemoveProfileImage(dto, true);

        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(userUtil.findUserById("user-1")).thenReturn(user);
        when(profileImage.isEmpty()).thenReturn(false);

        assertThatThrownBy(() -> userService.updateMyInfo(authentication, dto, profileImage))
                .isInstanceOf(org.apache.coyote.BadRequestException.class)
                .hasMessageContaining("cannot be requested together");
    }

    @Test
    @DisplayName("현재 유저 식별자 조회는 사용자 식별자를 반환한다")
    void getMyIdentifier_returnsUserId() {
        when(userUtil.extractUserId(authentication)).thenReturn("user-1");

        UserIdentifierResponseDto result = userService.getMyIdentifier(authentication);

        assertThat(result.userId()).isEqualTo("user-1");
    }

    private void setRemoveProfileImage(UpdateMyInfoRequestDto dto, boolean value) {
        try {
            Field field = UpdateMyInfoRequestDto.class.getDeclaredField("removeProfileImage");
            field.setAccessible(true);
            field.setBoolean(dto, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private com.project.ds_helper.common.dto.request.S3ImageUploadRequestDto compressedImage(
            String storedFilename,
            String originalFilename,
            String fileExtension,
            byte[] bytes
    ) {
        return com.project.ds_helper.common.dto.request.S3ImageUploadRequestDto.builder()
                .storedFilename(storedFilename)
                .originalFilename(originalFilename)
                .bytes(bytes)
                .size((long) bytes.length)
                .s3ContentType("image/" + fileExtension)
                .fileExtension(fileExtension)
                .build();
    }
}
