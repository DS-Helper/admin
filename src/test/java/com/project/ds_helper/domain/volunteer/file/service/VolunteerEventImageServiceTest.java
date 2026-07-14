package com.project.ds_helper.domain.volunteer.file.service;

import com.project.ds_helper.common.dto.request.S3ImageUploadRequestDto;
import com.project.ds_helper.common.exception.BusinessException;
import com.project.ds_helper.domain.post.util.ImageCompressionUtil;
import com.project.ds_helper.domain.post.util.S3Util;
import com.project.ds_helper.domain.volunteer.file.entity.VolunteerFile;
import com.project.ds_helper.domain.volunteer.file.repository.VolunteerFileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VolunteerEventImageServiceTest {

    @Mock private ImageCompressionUtil imageCompressionUtil;
    @Mock private S3Util s3Util;
    @Mock private VolunteerFileRepository fileRepository;
    @InjectMocks private VolunteerEventImageService service;

    @Test
    void upload_savesPublicEventImageAndReturnsPresignedUrl() throws Exception {
        MockMultipartFile image = new MockMultipartFile("image", "event.png", "image/png", png());
        S3ImageUploadRequestDto compressed = S3ImageUploadRequestDto.builder()
                .storedFilename("volunteer-event-id.webp")
                .originalFilename("event.png")
                .bytes(new byte[] {1})
                .size(1L)
                .s3ContentType("image/webp")
                .fileExtension("webp")
                .build();
        when(imageCompressionUtil.compressImage(any(), any())).thenReturn(compressed);
        when(s3Util.buildS3Key(any())).thenReturn("images/volunteer-event-id.webp");
        doNothing().when(s3Util).uploadImage(compressed);
        when(fileRepository.save(any(VolunteerFile.class))).thenAnswer(invocation -> {
            VolunteerFile file = invocation.getArgument(0);
            org.springframework.test.util.ReflectionTestUtils.setField(file, "id", "file-1");
            return file;
        });
        when(s3Util.toS3UrlByS3Key("images/volunteer-event-id.webp")).thenReturn("https://signed-url");

        var response = service.upload("admin-1", image);

        ArgumentCaptor<VolunteerFile> captor = ArgumentCaptor.forClass(VolunteerFile.class);
        verify(fileRepository).save(captor.capture());
        assertThat(captor.getValue().isPrivateFile()).isFalse();
        assertThat(captor.getValue().getOwnerUserId()).isEqualTo("admin-1");
        assertThat(captor.getValue().getContentType()).isEqualTo("image/webp");
        assertThat(response.url()).isEqualTo("https://signed-url");
        assertThat(response.width()).isEqualTo(1);
        assertThat(response.height()).isEqualTo(1);
    }

    @Test
    void upload_rejectsUnsupportedExtension() {
        MockMultipartFile image = new MockMultipartFile("image", "event.gif", "image/gif", new byte[] {1});

        assertThatThrownBy(() -> service.upload("admin-1", image))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("jpg, jpeg, png");
    }

    @Test
    void upload_deletesS3ObjectWhenMetadataSaveFails() throws Exception {
        MockMultipartFile image = new MockMultipartFile("image", "event.png", "image/png", png());
        S3ImageUploadRequestDto compressed = S3ImageUploadRequestDto.builder()
                .storedFilename("volunteer-event-id.webp")
                .originalFilename("event.png")
                .bytes(new byte[] {1})
                .size(1L)
                .s3ContentType("image/webp")
                .fileExtension("webp")
                .build();
        when(imageCompressionUtil.compressImage(any(), any())).thenReturn(compressed);
        when(s3Util.buildS3Key(any())).thenReturn("images/volunteer-event-id.webp");
        doNothing().when(s3Util).uploadImage(compressed);
        when(fileRepository.save(any(VolunteerFile.class))).thenThrow(new IllegalStateException("database failed"));

        assertThatThrownBy(() -> service.upload("admin-1", image))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("database failed");

        verify(s3Util).deleteImagesByS3Key(java.util.List.of("images/volunteer-event-id.webp"));
    }

    private byte[] png() throws Exception {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return output.toByteArray();
        }
    }
}
