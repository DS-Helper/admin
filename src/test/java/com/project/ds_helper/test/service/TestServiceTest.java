package com.project.ds_helper.test.service;

import com.project.ds_helper.common.dto.response.JwtResponse;
import com.project.ds_helper.common.dto.request.S3ImageUploadRequestDto;
import com.project.ds_helper.common.exception.user.UserNotFoundException;
import com.project.ds_helper.common.util.CookieUtil;
import com.project.ds_helper.common.util.JwtUtil;
import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.post.util.ImageCompressionUtil;
import com.project.ds_helper.domain.user.entity.User;
import com.project.ds_helper.domain.user.enums.UserRole;
import com.project.ds_helper.domain.user.enums.UserType;
import com.project.ds_helper.test.dto.TestImageCompressionPreviewResponseDto;
import com.project.ds_helper.test.repository.TestUserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestServiceTest {

    @Mock
    private TestUserRepository testUserRepository;

    @Mock
    private UserUtil userUtil;

    @Mock
    private CookieUtil cookieUtil;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Mock
    private ImageCompressionUtil imageCompressionUtil;

    @Mock
    private Authentication authentication;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private TestService testService;

    @Test
    @DisplayName("테스트 사용자 정보 조회는 DTO를 반환한다")
    void getUserInfo_returnsDto() {
        User user = User.builder().id("user-1").email("user@test.com").name("tester").build();
        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(userUtil.findUserById("user-1")).thenReturn(user);

        Object result = testService.getUserInfo(authentication);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("임시 토큰 발급은 관리자 계정을 찾지 못하면 예외가 발생한다")
    void generateTempToken_throwsWhenAdminMissing() {
        when(testUserRepository.findByEmail("admin@dshelper.kr")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> testService.generateTempToken(response))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("임시 토큰 발급은 JwtResponse를 반환한다")
    void generateTempToken_returnsJwtResponse() {
        User user = User.builder()
                .id("admin-id")
                .email("admin@dshelper.kr")
                .role(UserRole.ADMIN)
                .type(UserType.PERSONAL)
                .build();

        when(testUserRepository.findByEmail("admin@dshelper.kr")).thenReturn(Optional.of(user));
        when(jwtUtil.generateAccessToken("admin-id", "ADMIN", "PERSONAL")).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken("admin-id", "ADMIN", "PERSONAL")).thenReturn("refresh-token");
        when(jwtUtil.toRedisRefreshTokenKey("admin-id")).thenReturn("refreshToken:admin-id");
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        JwtResponse result = testService.generateTempToken(response);

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        verify(valueOperations).set("refreshToken:admin-id", "refresh-token");
    }

    @Test
    @DisplayName("관리자 생성은 이미 존재하면 예외가 발생한다")
    void createNewAdmin_throwsWhenAlreadyExists() {
        when(testUserRepository.existsByEmail("admin@dshelper.kr")).thenReturn(true);

        assertThatThrownBy(() -> testService.createNewAdmin())
                .isInstanceOf(org.apache.coyote.BadRequestException.class)
                .hasMessageContaining("Already Exists");
    }

    @Test
    @DisplayName("관리자 생성은 저장을 수행한다")
    void createNewAdmin_savesUser() throws Exception {
        when(testUserRepository.existsByEmail("admin@dshelper.kr")).thenReturn(false);
        when(bCryptPasswordEncoder.encode("adminadmin")).thenReturn("encoded");

        testService.createNewAdmin();

        verify(testUserRepository).save(any(User.class));
    }

    @Test
    @DisplayName("이미지 압축 미리보기는 원본과 압축 결과를 모두 data URL로 반환한다")
    void previewCompressedImage_returnsOriginalAndCompressedPreview() throws Exception {
        MockMultipartFile image = new MockMultipartFile("image", "sample.png", "image/png", "abc".getBytes());
        S3ImageUploadRequestDto compressedImage = S3ImageUploadRequestDto.builder()
                .storedFilename("test-preview")
                .originalFilename("sample.png")
                .bytes("xy".getBytes())
                .size(2L)
                .s3ContentType("image/webp")
                .fileExtension("webp")
                .build();
        when(imageCompressionUtil.compressImage(image, "test-preview")).thenReturn(compressedImage);

        TestImageCompressionPreviewResponseDto result = testService.previewCompressedImage(image);

        assertThat(result.originalFilename()).isEqualTo("sample.png");
        assertThat(result.originalContentType()).isEqualTo("image/png");
        assertThat(result.originalImageDataUrl()).startsWith("data:image/png;base64,");
        assertThat(result.compressedStoredFilename()).isEqualTo("test-preview");
        assertThat(result.compressedContentType()).isEqualTo("image/webp");
        assertThat(result.compressedExtension()).isEqualTo("webp");
        assertThat(result.compressedImageDataUrl()).startsWith("data:image/webp;base64,");
    }

    @Test
    @DisplayName("압축 바이너리 미리보기는 실제 WebP 업로드 DTO를 그대로 반환한다")
    void previewCompressedImageBinary_returnsCompressedDto() throws Exception {
        MockMultipartFile image = new MockMultipartFile("image", "sample.png", "image/png", "abc".getBytes());
        S3ImageUploadRequestDto compressedImage = S3ImageUploadRequestDto.builder()
                .storedFilename("test-preview")
                .originalFilename("sample.png")
                .bytes("xy".getBytes())
                .size(2L)
                .s3ContentType("image/webp")
                .fileExtension("webp")
                .build();
        when(imageCompressionUtil.compressImage(image, "test-preview")).thenReturn(compressedImage);

        S3ImageUploadRequestDto result = testService.previewCompressedImageBinary(image);

        assertThat(result).isEqualTo(compressedImage);
    }
}
