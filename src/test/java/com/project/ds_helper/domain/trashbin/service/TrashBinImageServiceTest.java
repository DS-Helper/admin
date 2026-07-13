package com.project.ds_helper.domain.trashbin.service;

import com.project.ds_helper.common.dto.request.S3ImageUploadRequestDto;
import com.project.ds_helper.domain.post.util.ImageCompressionUtil;
import com.project.ds_helper.domain.post.util.ImageUtil;
import com.project.ds_helper.domain.post.util.S3Util;
import com.project.ds_helper.domain.trashbin.dto.response.UploadTrashBinImageResponseDto;
import com.project.ds_helper.domain.trashbin.entity.TrashBin;
import com.project.ds_helper.domain.trashbin.entity.TrashBinImage;
import com.project.ds_helper.domain.trashbin.repository.TrashBinImageRepository;
import com.project.ds_helper.domain.trashbin.repository.TrashBinRepository;
import com.project.ds_helper.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrashBinImageServiceTest {

    @Mock private TrashBinRepository trashBinRepository;
    @Mock private TrashBinImageRepository trashBinImageRepository;
    @Mock private ImageUtil imageUtil;
    @Mock private ImageCompressionUtil imageCompressionUtil;
    @Mock private S3Util s3Util;

    @InjectMocks
    private TrashBinImageService trashBinImageService;

    @Test
    @DisplayName("이미지 업로드는 쓰레기통 이미지 메타데이터를 저장한다")
    void uploadTrashBinImage_uploadsImage() throws Exception {
        TrashBin trashBin = TrashBin.builder().id("bin-1").latitude(35.808057).longitude(128.508827).build();
        MockMultipartFile image = new MockMultipartFile("image", "35.808057.png", "image/png", "image".getBytes(StandardCharsets.UTF_8));
        S3ImageUploadRequestDto dto = S3ImageUploadRequestDto.builder()
                .storedFilename("stored.png").originalFilename("35.808057.png").bytes(new byte[]{1}).size(1L)
                .s3ContentType("image/png").fileExtension("png").build();

        when(trashBinRepository.findAllByLatitude(35.808057)).thenReturn(List.of(trashBin));
        when(trashBinImageRepository.findFirstByTrashBin_Id("bin-1")).thenReturn(Optional.empty());
        when(imageUtil.toStoredFilename()).thenReturn("stored.png");
        when(imageCompressionUtil.compressImage(any(), any())).thenReturn(dto);
        doNothing().when(s3Util).uploadImage(dto);
        when(s3Util.buildS3Key("stored.png")).thenReturn("images/stored.png");
        when(s3Util.toS3UrlByS3Key("images/stored.png")).thenReturn("https://s3/images/stored.png");
        when(trashBinImageRepository.save(any(TrashBinImage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = trashBinImageService.uploadTrashBinImage(image);

        assertThat(result.imageUrl()).isEqualTo("https://s3/images/stored.png");
    }

    @Test
    @DisplayName("기존 이미지가 있으면 이미 존재한다고 반환한다")
    void uploadTrashBinImage_returnsAlreadyExists() throws Exception {
        TrashBin trashBin = TrashBin.builder().id("bin-1").latitude(35.808057).longitude(128.508827).build();
        MockMultipartFile image = new MockMultipartFile("image", "35.808057.png", "image/png", "image".getBytes(StandardCharsets.UTF_8));

        when(trashBinRepository.findAllByLatitude(35.808057)).thenReturn(List.of(trashBin));
        when(trashBinImageRepository.findFirstByTrashBin_Id("bin-1")).thenReturn(Optional.of(TrashBinImage.builder().trashBin(trashBin).build()));

        var result = trashBinImageService.uploadTrashBinImage(image);

        assertThat(result.alreadyExists()).isTrue();
    }

    @Test
    @DisplayName("여러 이미지는 순차 업로드된다")
    void uploadTrashBinImages_uploadsMany() throws Exception {
        TrashBin trashBin = TrashBin.builder().id("bin-1").latitude(35.808057).longitude(128.508827).build();
        MockMultipartFile image = new MockMultipartFile("image", "35.808057.png", "image/png", "image".getBytes(StandardCharsets.UTF_8));
        S3ImageUploadRequestDto dto = S3ImageUploadRequestDto.builder()
                .storedFilename("stored.png").originalFilename("35.808057.png").bytes(new byte[]{1}).size(1L)
                .s3ContentType("image/png").fileExtension("png").build();

        when(trashBinRepository.findAllByLatitude(35.808057)).thenReturn(List.of(trashBin));
        when(trashBinImageRepository.findFirstByTrashBin_Id("bin-1")).thenReturn(Optional.empty());
        when(imageUtil.toStoredFilename()).thenReturn("stored.png");
        when(imageCompressionUtil.compressImage(any(), any())).thenReturn(dto);
        doNothing().when(s3Util).uploadImage(dto);
        when(s3Util.buildS3Key("stored.png")).thenReturn("images/stored.png");
        when(s3Util.toS3UrlByS3Key("images/stored.png")).thenReturn("https://s3/images/stored.png");
        when(trashBinImageRepository.save(any(TrashBinImage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = trashBinImageService.uploadTrashBinImages(List.of(image));

        assertThat(result.images()).hasSize(1);
    }

    @Test
    @DisplayName("이미지 업로드는 파일이 없으면 예외가 발생한다")
    void uploadTrashBinImage_throwsWhenImageMissing() {
        assertThatThrownBy(() -> trashBinImageService.uploadTrashBinImage(null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("파일명이 위도 형식이 아니면 예외가 발생한다")
    void uploadTrashBinImage_throwsWhenFilenameIsInvalid() {
        MockMultipartFile image = new MockMultipartFile("image", "invalid-name.png", "image/png", "image".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> trashBinImageService.uploadTrashBinImage(image))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("image filename must be latitude value");
    }

    @Test
    @DisplayName("복수 이미지 업로드는 빈 입력이면 예외가 발생한다")
    void uploadTrashBinImages_throwsWhenListEmpty() {
        assertThatThrownBy(() -> trashBinImageService.uploadTrashBinImages(List.of()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("images are required");
    }

    @Test
    @DisplayName("파일명이 비어 있으면 예외가 발생한다")
    void uploadTrashBinImage_throwsWhenFilenameBlank() {
        MockMultipartFile image = new MockMultipartFile("image", "", "image/png", "image".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> trashBinImageService.uploadTrashBinImage(image))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("image filename is required");
    }

    @Test
    @DisplayName("위도에 해당하는 쓰레기통이 없으면 예외가 발생한다")
    void uploadTrashBinImage_throwsWhenTrashBinMissing() {
        MockMultipartFile image = new MockMultipartFile("image", "35.808057.png", "image/png", "image".getBytes(StandardCharsets.UTF_8));
        when(trashBinRepository.findAllByLatitude(35.808057)).thenReturn(List.of());

        assertThatThrownBy(() -> trashBinImageService.uploadTrashBinImage(image))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("No trash bin found by latitude");
    }

    @Test
    @DisplayName("위도에 해당하는 쓰레기통이 여러 개면 예외가 발생한다")
    void uploadTrashBinImage_throwsWhenTrashBinDuplicated() {
        MockMultipartFile image = new MockMultipartFile("image", "35.808057.png", "image/png", "image".getBytes(StandardCharsets.UTF_8));
        TrashBin trashBin = TrashBin.builder().id("bin-1").latitude(35.808057).longitude(128.508827).build();
        TrashBin another = TrashBin.builder().id("bin-2").latitude(35.808057).longitude(128.508828).build();
        when(trashBinRepository.findAllByLatitude(35.808057)).thenReturn(List.of(trashBin, another));

        assertThatThrownBy(() -> trashBinImageService.uploadTrashBinImage(image))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Multiple trash bins found by latitude");
    }

    @Test
    @DisplayName("파일명에 확장자가 없어도 위도 파싱이 된다")
    void parseLatitudeFromFilename_parsesWithoutExtension() {
        Double latitude = ReflectionTestUtils.invokeMethod(trashBinImageService, "parseLatitudeFromFilename", "35.808057");

        assertThat(latitude).isNotNull();
    }

    @Test
    @DisplayName("롤백 삭제는 삭제 예외가 발생해도 흡수한다")
    void rollbackUploadedImage_ignoresDeleteFailure() {
        doThrow(new RuntimeException("delete failed")).when(s3Util).deleteImage("stored.png");

        ReflectionTestUtils.invokeMethod(trashBinImageService, "rollbackUploadedImage", "stored.png");

        verify(s3Util).deleteImage("stored.png");
    }

    @Test
    @DisplayName("복수 롤백은 이미 존재한다고 표시된 이미지를 삭제하지 않는다")
    void rollbackUploadedImages_skipsAlreadyExists() {
        UploadTrashBinImageResponseDto alreadyExists = UploadTrashBinImageResponseDto.builder()
                .alreadyExists(true)
                .storedName("stored-existing")
                .build();
        UploadTrashBinImageResponseDto uploaded = UploadTrashBinImageResponseDto.builder()
                .alreadyExists(false)
                .storedName("stored-uploaded")
                .build();

        ReflectionTestUtils.invokeMethod(trashBinImageService, "rollbackUploadedImages", List.of(alreadyExists, uploaded));

        verify(s3Util, never()).deleteImage("stored-existing");
        verify(s3Util).deleteImage("stored-uploaded");
    }

    @Test
    @DisplayName("위도 조회가 null이면 예외가 발생한다")
    void uploadTrashBinImage_throwsWhenLatitudeNull() {
        MockMultipartFile image = new MockMultipartFile("image", "null.png", "image/png", "image".getBytes(StandardCharsets.UTF_8));
        when(trashBinRepository.findAllByLatitude(null)).thenReturn(List.of());

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(trashBinImageService, "findTrashBinByLatitude", (Double) null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("No trash bin found by latitude");
    }
}
