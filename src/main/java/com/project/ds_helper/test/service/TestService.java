package com.project.ds_helper.test.service;

import com.project.ds_helper.common.dto.request.S3ImageUploadRequestDto;
import com.project.ds_helper.common.dto.response.JwtResponse;
import com.project.ds_helper.common.exception.user.UserNotFoundException;
import com.project.ds_helper.common.util.CookieUtil;
import com.project.ds_helper.common.util.JwtUtil;
import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.post.util.ImageCompressionUtil;
import com.project.ds_helper.domain.user.entity.User;
import com.project.ds_helper.domain.user.enums.UserRole;
import com.project.ds_helper.domain.user.enums.UserType;
import com.project.ds_helper.test.dto.TestImageCompressionPreviewResponseDto;
import com.project.ds_helper.test.dto.TestUserInfo;
import com.project.ds_helper.test.repository.TestUserRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;

@Slf4j
@Service
public class TestService {

    private final TestUserRepository testUserRepository;
    private final UserUtil userUtil;
    private final CookieUtil cookieUtil;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final ImageCompressionUtil imageCompressionUtil;

    public TestService(TestUserRepository testUserRepository, UserUtil userUtil, CookieUtil cookieUtil, JwtUtil jwtUtil, @Qualifier(value = "CustomStringRedisTemplate") StringRedisTemplate stringRedisTemplate, BCryptPasswordEncoder bCryptPasswordEncoder, ImageCompressionUtil imageCompressionUtil) {
        this.testUserRepository = testUserRepository;
        this.userUtil = userUtil;
        this.cookieUtil = cookieUtil;
        this.jwtUtil = jwtUtil;
        this.stringRedisTemplate = stringRedisTemplate;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
        this.imageCompressionUtil = imageCompressionUtil;
    }

    /**
     * 유저 정보 조회
     * **/
    public Object getUserInfo(Authentication authentication) {
        return TestUserInfo.toDto(userUtil.findUserById(userUtil.extractUserId(authentication)));
    }

    /**
     * 개발용 임시 JWT 토큰 발급
     *
     * DB에서 ADMIN 게정을 조회하여 JWT 토큰 발급
     * **/
    public JwtResponse generateTempToken(HttpServletResponse response) {

        String email = "admin@dshelper.kr";

        User user = testUserRepository.findByEmail(email).orElseThrow( () -> new UserNotFoundException("ADMIN is not found"));
        String userId = user.getId();
        String role =  user.getRole().name();
        String type = user.getType().name();
        log.info("useId : {}, role : {}, type : {}", userId, role, type);

        String accessToken = jwtUtil.generateAccessToken(userId, role, type);
        String refreshToken = jwtUtil.generateRefreshToken(userId, role, type);
        log.info("accessToken : {}, refreshToken : {}", accessToken, refreshToken);
        stringRedisTemplate.opsForValue().set(jwtUtil.toRedisRefreshTokenKey(userId), refreshToken);

        return new JwtResponse(accessToken, refreshToken);
    }

    /**
     * 신규 관리자 게정 생성
     *
     * **/
    public void createNewAdmin() throws BadRequestException {
        
        String email = "admin@dshelper.kr";
        
        if(testUserRepository.existsByEmail(email)) throw new BadRequestException("Already Exists : " + email);

        User user = User.builder()
                .email(email)
                .role(UserRole.ADMIN)
                .type(UserType.PERSONAL)
                .name("ADMIN")
                .password(bCryptPasswordEncoder.encode("adminadmin"))
                .build();

        testUserRepository.save(user);
    }

    /**
     * 업로드한 원본 이미지와 WebP 압축 결과를 동시에 확인하는 테스트용 메서드
     */
    public TestImageCompressionPreviewResponseDto previewCompressedImage(MultipartFile image) throws IOException {
        log.debug("TestService.previewCompressedImage started. originalFilename={}, contentType={}, size={}",
                image == null ? null : image.getOriginalFilename(),
                image == null ? null : image.getContentType(),
                image == null ? null : image.getSize());

        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Image file is empty");
        }

        // 1. 실제 서비스와 동일한 압축 유틸을 사용해 WebP 결과를 만든다.
        String storedFilename = "test-preview";
        var compressedImage = imageCompressionUtil.compressImage(image, storedFilename);

        // 2. 원본/압축 이미지를 바로 확인할 수 있도록 둘 다 data URL로 변환한다.
        String originalImageDataUrl = toDataUrl(image.getContentType(), image.getBytes());
        String compressedImageDataUrl = toDataUrl(compressedImage.getS3ContentType(), compressedImage.getBytes());

        TestImageCompressionPreviewResponseDto responseDto = TestImageCompressionPreviewResponseDto.builder()
                .originalFilename(image.getOriginalFilename())
                .originalContentType(image.getContentType())
                .originalSize(image.getSize())
                .originalImageDataUrl(originalImageDataUrl)
                .compressedStoredFilename(compressedImage.getStoredFilename())
                .compressedContentType(compressedImage.getS3ContentType())
                .compressedExtension(compressedImage.getFileExtension())
                .compressedSize(compressedImage.getSize())
                .compressedImageDataUrl(compressedImageDataUrl)
                .build();

        log.debug("TestService.previewCompressedImage completed. originalSize={}, compressedSize={}, compressedContentType={}",
                responseDto.originalSize(), responseDto.compressedSize(), responseDto.compressedContentType());
        return responseDto;
    }

    /**
     * 클라이언트가 실제 WebP 바이너리를 직접 내려받아 확인할 수 있도록 압축 결과 원본 바이트를 반환한다.
     */
    public S3ImageUploadRequestDto previewCompressedImageBinary(MultipartFile image) throws IOException {
        log.debug("TestService.previewCompressedImageBinary started. originalFilename={}, contentType={}, size={}",
                image == null ? null : image.getOriginalFilename(),
                image == null ? null : image.getContentType(),
                image == null ? null : image.getSize());

        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Image file is empty");
        }

        S3ImageUploadRequestDto compressedImage = imageCompressionUtil.compressImage(image, "test-preview");
        log.debug("TestService.previewCompressedImageBinary completed. compressedSize={}, compressedContentType={}",
                compressedImage.getSize(), compressedImage.getS3ContentType());
        return compressedImage;
    }

    private String toDataUrl(String contentType, byte[] bytes) {
        return "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(bytes);
    }
}
