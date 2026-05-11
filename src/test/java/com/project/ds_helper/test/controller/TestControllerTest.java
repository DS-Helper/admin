package com.project.ds_helper.test.controller;

import com.project.ds_helper.common.dto.request.S3ImageUploadRequestDto;
import com.project.ds_helper.common.dto.response.JwtResponse;
import com.project.ds_helper.test.dto.TestImageCompressionPreviewResponseDto;
import com.project.ds_helper.test.service.TestService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestControllerTest {

    @Mock
    private TestService testService;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private TestController testController;

    @Test
    @DisplayName("테스트 사용자 정보 조회는 서비스 결과를 반환한다")
    void getUserInfo_returnsServiceResult() {
        when(testService.getUserInfo(null)).thenReturn("info");

        ResponseEntity<?> result = testController.getUserInfo(null);

        assertThat(result.getBody()).isEqualTo("info");
    }

    @Test
    @DisplayName("관리자 생성은 201을 반환한다")
    void createNewAdmin_returnsCreated() throws Exception {
        ResponseEntity<?> result = testController.createNewAdmin();

        verify(testService).createNewAdmin();
        assertThat(result.getStatusCode().value()).isEqualTo(201);
    }

    @Test
    @DisplayName("임시 토큰 발급은 JwtResponse를 반환한다")
    void tempToken_returnsJwtResponse() {
        JwtResponse jwtResponse = new JwtResponse("access-token", "refresh-token");
        when(testService.generateTempToken(response)).thenReturn(jwtResponse);

        ResponseEntity<JwtResponse> result = testController.tempToken(response);

        verify(testService).generateTempToken(response);
        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isEqualTo(jwtResponse);
    }

    @Test
    @DisplayName("이미지 압축 미리보기는 원본/압축 응답 DTO를 반환한다")
    void previewCompressedImage_returnsPreviewDto() throws Exception {
        MockMultipartFile image = new MockMultipartFile("image", "sample.png", "image/png", "abc".getBytes());
        TestImageCompressionPreviewResponseDto previewResponseDto = TestImageCompressionPreviewResponseDto.builder()
                .originalFilename("sample.png")
                .originalContentType("image/png")
                .originalSize(3L)
                .originalImageDataUrl("data:image/png;base64,YWJj")
                .compressedStoredFilename("test-preview")
                .compressedContentType("image/webp")
                .compressedExtension("webp")
                .compressedSize(2L)
                .compressedImageDataUrl("data:image/webp;base64,AAA=")
                .build();
        when(testService.previewCompressedImage(image)).thenReturn(previewResponseDto);

        ResponseEntity<TestImageCompressionPreviewResponseDto> result = testController.previewCompressedImage(image);

        verify(testService).previewCompressedImage(image);
        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isEqualTo(previewResponseDto);
    }

    @Test
    @DisplayName("압축 바이너리 미리보기는 WebP 응답 헤더와 바이트를 반환한다")
    void previewCompressedImageBinary_returnsBinaryResponse() throws Exception {
        MockMultipartFile image = new MockMultipartFile("image", "sample.png", "image/png", "abc".getBytes());
        S3ImageUploadRequestDto compressedImage = S3ImageUploadRequestDto.builder()
                .storedFilename("test-preview")
                .originalFilename("sample.png")
                .bytes("compressed".getBytes())
                .size(10L)
                .s3ContentType("image/webp")
                .fileExtension("webp")
                .build();
        when(testService.previewCompressedImageBinary(image)).thenReturn(compressedImage);

        ResponseEntity<ByteArrayResource> result = testController.previewCompressedImageBinary(image);

        verify(testService).previewCompressedImageBinary(image);
        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)).isEqualTo("inline; filename=\"test-preview.webp\"");
        assertThat(result.getHeaders().getContentType().toString()).isEqualTo("image/webp");
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().contentLength()).isEqualTo(10L);
    }
}
