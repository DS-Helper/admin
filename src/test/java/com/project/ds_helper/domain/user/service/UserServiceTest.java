package com.project.ds_helper.domain.user.service;

import com.project.ds_helper.common.util.PasswordUtil;
import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.post.util.ImageCompressionUtil;
import com.project.ds_helper.domain.post.util.ImageUtil;
import com.project.ds_helper.domain.post.util.S3Util;
import com.project.ds_helper.domain.user.dto.request.OrganizationJoinReqDto;
import com.project.ds_helper.domain.user.dto.request.OrganizationLoginReqDto;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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
    private ImageCompressionUtil imageCompressionUtil;

    @Mock
    private ImageUtil imageUtil;

    @Mock
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Mock
    private UserUtil userUtil;

    @Mock
    private UserProfileService userProfileService;

    @Mock
    private UserAuthTokenService userAuthTokenService;

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
        when(userAuthTokenService.checkIfUserLoggedIn("")).thenReturn(false);
        Object result = userService.checkIfUserLoggedIn("");

        assertThat(result).isEqualTo(false);
    }

    @Test
    @DisplayName("로그인 체크는 만료된 refresh token 이면 false를 반환한다")
    void checkIfUserLoggedIn_returnsFalseWhenTokenExpired() {
        when(userAuthTokenService.checkIfUserLoggedIn("refresh-token")).thenReturn(false);

        Object result = userService.checkIfUserLoggedIn("refresh-token");

        assertThat(result).isEqualTo(false);
    }

    @Test
    @DisplayName("기관 회원 가입은 인증서 이미지가 있으면 업로드 후 저장한다")
    void organizationJoin_savesOrganizationWithCertificates() throws Exception {
        OrganizationJoinReqDto dto = OrganizationJoinReqDto.builder()
                .email("org@test.com")
                .password("pw")
                .passwordCheck("pw")
                .organizationName("기관")
                .organizationPhoneNumber("010-1234-5678")
                .build();
        MultipartFile certification = new MockMultipartFile("cert", "cert.png", "image/png", new byte[]{1});
        when(passwordUtil.isPasswordMatch("pw", "pw")).thenReturn(true);
        when(bCryptPasswordEncoder.encode("pw")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", "user-1");
            return saved;
        });
        when(imageUtil.toStoredFilename()).thenReturn("stored.png");
        when(imageCompressionUtil.compressImage(any(), any())).thenReturn(compressedImage("stored.png", "cert.png", "png", new byte[]{1}));
        when(s3Util.uploadImages(anyList())).thenReturn(java.util.List.of("https://bucket/cert.png"));

        userService.organizationJoin(dto, java.util.List.of(certification));

        verify(organizationRepository).save(any());
    }

    @Test
    @DisplayName("기관 회원 가입은 인증서가 없으면 이미지 업로드 없이 저장한다")
    void organizationJoin_savesOrganizationWithoutCertificates() throws Exception {
        OrganizationJoinReqDto dto = OrganizationJoinReqDto.builder()
                .email("org@test.com")
                .password("pw")
                .passwordCheck("pw")
                .organizationName("기관")
                .organizationPhoneNumber("010-1234-5678")
                .build();
        when(passwordUtil.isPasswordMatch("pw", "pw")).thenReturn(true);
        when(bCryptPasswordEncoder.encode("pw")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", "user-1");
            return saved;
        });

        userService.organizationJoin(dto, null);

        verify(organizationRepository).save(any());
        verify(s3Util, never()).uploadImages(anyList());
    }

    @Test
    @DisplayName("기관 로그인은 정상 계정이면 토큰을 발급한다")
    void organizationLogin_generatesTokens() {
        OrganizationLoginReqDto dto = OrganizationLoginReqDto.builder().email("org@test.com").password("pw").build();
        User user = User.builder().id("user-1").password("encoded").type(UserType.ORGANIZATION).build();
        when(userRepository.findByEmail("org@test.com")).thenReturn(java.util.Optional.of(user));
        when(bCryptPasswordEncoder.matches("pw", "encoded")).thenReturn(true);

        userService.organizationLogin(dto, response);

        verify(userAuthTokenService).generateJwtTokenAndPutInResponseHeader(response, "user-1", "USER", "ORGANIZATION");
    }

    @Test
    @DisplayName("기관 로그인은 계정이 없으면 예외가 발생한다")
    void organizationLogin_throwsWhenUserMissing() {
        OrganizationLoginReqDto dto = OrganizationLoginReqDto.builder().email("org@test.com").password("pw").build();
        when(userRepository.findByEmail("org@test.com")).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> userService.organizationLogin(dto, response))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Organization Not Found");
    }

    @Test
    @DisplayName("기관 로그인은 기관 계정이 아니면 예외가 발생한다")
    void organizationLogin_throwsWhenNotOrganization() {
        OrganizationLoginReqDto dto = OrganizationLoginReqDto.builder().email("org@test.com").password("pw").build();
        User user = User.builder().id("user-1").password("encoded").type(UserType.PERSONAL).build();
        when(userRepository.findByEmail("org@test.com")).thenReturn(java.util.Optional.of(user));

        assertThatThrownBy(() -> userService.organizationLogin(dto, response))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Not Organization");
    }

    @Test
    @DisplayName("기관 로그인은 비밀번호가 다르면 예외가 발생한다")
    void organizationLogin_throwsWhenWrongPassword() {
        OrganizationLoginReqDto dto = OrganizationLoginReqDto.builder().email("org@test.com").password("pw").build();
        User user = User.builder().id("user-1").password("encoded").type(UserType.ORGANIZATION).build();
        when(userRepository.findByEmail("org@test.com")).thenReturn(java.util.Optional.of(user));
        when(bCryptPasswordEncoder.matches("pw", "encoded")).thenReturn(false);

        assertThatThrownBy(() -> userService.organizationLogin(dto, response))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Wrong Password");
    }

    @Test
    @DisplayName("기관 로그인은 탈퇴된 계정이면 예외가 발생한다")
    void organizationLogin_throwsWhenDeletedUser() {
        OrganizationLoginReqDto dto = OrganizationLoginReqDto.builder().email("org@test.com").password("pw").build();
        User user = User.builder().id("user-1").password("encoded").type(UserType.ORGANIZATION).build();
        user.softDelete(java.time.LocalDateTime.now());
        when(userRepository.findByEmail("org@test.com")).thenReturn(java.util.Optional.of(user));

        assertThatThrownBy(() -> userService.organizationLogin(dto, response))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Deleted User");
    }

    @Test
    @DisplayName("현재 유저 역할 조회는 인증 정보가 없으면 null을 반환한다")
    void getMyIdentifier_returnsNullRoleWhenAuthenticationMissing() {
        when(userUtil.extractUserId(authentication)).thenReturn("user-1");

        UserIdentifierResponseDto result = userService.getMyIdentifier(authentication);

        assertThat(result.userRole()).isNull();
    }

    @Test
    @DisplayName("현재 유저 역할 조회는 권한이 있으면 첫 권한을 반환한다")
    void getMyIdentifier_returnsAuthorityWhenPresent() {
        Authentication auth = org.mockito.Mockito.mock(Authentication.class);
        when(userUtil.extractUserId(auth)).thenReturn("user-1");
        when(auth.getAuthorities()).thenReturn((java.util.Collection) java.util.List.of((GrantedAuthority) () -> "ROLE_USER"));

        UserIdentifierResponseDto result = userService.getMyIdentifier(auth);

        assertThat(result.userRole()).isEqualTo("ROLE_USER");
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
    @DisplayName("일반 회원 가입은 정상 입력이면 토큰 발급까지 진행한다")
    void userJoin_generatesTokens() throws Exception {
        UserJoinReqDto dto = UserJoinReqDto.builder()
                .email("user@test.com")
                .password("pw")
                .passwordCheck("pw")
                .build();
        when(userUtil.existsByEmail("user@test.com")).thenReturn(false);
        when(bCryptPasswordEncoder.encode("pw")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", "user-1");
            return saved;
        });

        userService.userJoin(dto, response);

        verify(userAuthTokenService).generateJwtTokenAndPutInResponseHeader(response, "user-1", "USER", "PERSONAL");
    }

    @Test
    @DisplayName("일반 회원 가입은 비밀번호가 다르면 예외가 발생한다")
    void userJoin_throwsWhenPasswordMismatch() {
        UserJoinReqDto dto = UserJoinReqDto.builder()
                .email("user@test.com")
                .password("pw1")
                .passwordCheck("pw2")
                .build();
        when(userUtil.existsByEmail("user@test.com")).thenReturn(false);

        assertThatThrownBy(() -> userService.userJoin(dto, response))
                .isInstanceOf(org.apache.coyote.BadRequestException.class)
                .hasMessageContaining("Password Not Matches");
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
        UserGetSelfInfoAtMyPageResponseDto dto = new UserGetSelfInfoAtMyPageResponseDto("홍길동", "user@test.com", null, null, null, null);
        when(userProfileService.getMyInfo(authentication)).thenReturn(dto);

        UserGetSelfInfoAtMyPageResponseDto result = userService.getMyInfo(authentication);

        assertThat(result.email()).isEqualTo("user@test.com");
        assertThat(result.name()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("새 프로필 이미지가 없고 삭제 요청도 아니면 기존 프로필 이미지를 유지한다")
    void updateMyInfo_keepsExistingProfileImage() throws Exception {
        UpdateMyInfoRequestDto dto = new UpdateMyInfoRequestDto();
        UserGetSelfInfoAtMyPageResponseDto dtoResult = new UserGetSelfInfoAtMyPageResponseDto("홍길동", "user@test.com", "1998", "male", "010-1234-5678", "https://bucket.s3.ap-northeast-2.amazonaws.com/images/current.png");
        when(userProfileService.updateMyInfo(authentication, dto, null)).thenReturn(dtoResult);

        UserGetSelfInfoAtMyPageResponseDto result = userService.updateMyInfo(authentication, dto, null);

        assertThat(result.profileImageUrl()).isEqualTo("https://bucket.s3.ap-northeast-2.amazonaws.com/images/current.png");
    }

    @Test
    @DisplayName("새 프로필 이미지가 있으면 기존 관리 이미지 삭제 후 새 이미지로 교체한다")
    void updateMyInfo_replacesProfileImage() throws Exception {
        UpdateMyInfoRequestDto dto = new UpdateMyInfoRequestDto();
        UserGetSelfInfoAtMyPageResponseDto dtoResult = new UserGetSelfInfoAtMyPageResponseDto("홍길동", "user@test.com", "1998", "male", "010-1234-5678", "https://bucket.s3.ap-northeast-2.amazonaws.com/images/new-profile.png");
        when(userProfileService.updateMyInfo(authentication, dto, profileImage)).thenReturn(dtoResult);

        UserGetSelfInfoAtMyPageResponseDto result = userService.updateMyInfo(authentication, dto, profileImage);

        assertThat(result.profileImageUrl()).isEqualTo("https://bucket.s3.ap-northeast-2.amazonaws.com/images/new-profile.png");
    }

    @Test
    @DisplayName("프로필 이미지 삭제 요청이면 기존 관리 이미지를 삭제하고 null로 변경한다")
    void updateMyInfo_removesProfileImage() throws Exception {
        UpdateMyInfoRequestDto dto = new UpdateMyInfoRequestDto();
        setRemoveProfileImage(dto, true);
        UserGetSelfInfoAtMyPageResponseDto dtoResult = new UserGetSelfInfoAtMyPageResponseDto("홍길동", "user@test.com", "1998", "male", "010-1234-5678", null);
        when(userProfileService.updateMyInfo(authentication, dto, null)).thenReturn(dtoResult);

        UserGetSelfInfoAtMyPageResponseDto result = userService.updateMyInfo(authentication, dto, null);

        assertThat(result.profileImageUrl()).isNull();
    }

    @Test
    @DisplayName("프로필 이미지 삭제와 새 파일 업로드를 동시에 요청하면 예외가 발생한다")
    void updateMyInfo_throwsWhenRemoveAndUploadRequestedTogether() throws Exception {
        UpdateMyInfoRequestDto dto = new UpdateMyInfoRequestDto();
        setRemoveProfileImage(dto, true);
        when(userProfileService.updateMyInfo(authentication, dto, profileImage))
                .thenThrow(new org.apache.coyote.BadRequestException("Profile image remove and upload cannot be requested together"));

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
