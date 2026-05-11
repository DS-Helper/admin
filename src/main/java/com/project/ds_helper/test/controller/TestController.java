package com.project.ds_helper.test.controller;

import com.project.ds_helper.common.dto.response.JwtResponse;
import com.project.ds_helper.common.enums.SwaggerTagName;
import com.project.ds_helper.test.dto.TestImageCompressionPreviewResponseDto;
import com.project.ds_helper.test.service.TestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

    private final TestService testService;

    @Tag(name = SwaggerTagName.TEST)
    @Operation(summary = "테스트 유저 정보 조회", description = "유저 정보 조회")
    @GetMapping("/user-info")
    public ResponseEntity<?> getUserInfo(Authentication authentication) {
        return ResponseEntity.ok(testService.getUserInfo(authentication));
    }

    @Tag(name = SwaggerTagName.TEST)
    @Operation(summary = "테스트 관리자 계정 생성", description = "관리자 계정 신규 생성")
    @PostMapping("/new-admin")
    public ResponseEntity<?> createNewAdmin() throws BadRequestException {
        testService.createNewAdmin();
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @Tag(name = SwaggerTagName.TEST)
    @Operation(summary = "테스트 임시 JWT 발급", description = "개발용 임시 JWT 토큰 발급")
    @GetMapping("/temp-token")
    public ResponseEntity<JwtResponse> tempToken(HttpServletResponse response) {
        return ResponseEntity.ok(testService.generateTempToken(response));
    }

    @Tag(name = SwaggerTagName.TEST)
    @Operation(summary = "이미지 압축 결과 미리보기", description = "업로드한 원본 이미지와 WebP 압축 결과를 함께 반환")
    @PostMapping("/image-compression-preview")
    public ResponseEntity<TestImageCompressionPreviewResponseDto> previewCompressedImage(
            @RequestPart("image") MultipartFile image
    ) throws Exception {
        return ResponseEntity.ok(testService.previewCompressedImage(image));
    }

    @Tag(name = SwaggerTagName.TEST)
    @Operation(summary = "압축 이미지 바이너리 다운로드", description = "업로드한 이미지를 WebP 손실 압축한 실제 바이너리를 바로 반환")
    @PostMapping("/image-compression-preview/binary")
    public ResponseEntity<ByteArrayResource> previewCompressedImageBinary(
            @RequestPart("image") MultipartFile image
    ) throws Exception {
        var compressedImage = testService.previewCompressedImageBinary(image);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + compressedImage.getStoredFilename() + "." + compressedImage.getFileExtension() + "\"")
                .contentType(MediaType.parseMediaType(compressedImage.getS3ContentType()))
                .contentLength(compressedImage.getSize())
                .body(new ByteArrayResource(compressedImage.getBytes()));
    }
}
