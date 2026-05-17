package com.project.ds_helper.domain.post.util;

import com.project.ds_helper.common.dto.request.S3ImageUploadRequestDto;
import com.project.ds_helper.domain.post.entity.PostImage;
import com.project.ds_helper.domain.post.repository.ImageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageUtilTest {

    @Mock
    private S3Util s3Util;

    @Mock
    private ImageRepository imageRepository;

    @Test
    @DisplayName("압축 이미지 DTO를 PostImage로 변환한다")
    void toPostImage_returnsPostImage() {
        ImageUtil imageUtil = new ImageUtil(s3Util, imageRepository);
        S3ImageUploadRequestDto dto = S3ImageUploadRequestDto.builder()
                .storedFilename("stored.webp")
                .originalFilename("original.png")
                .size(10L)
                .fileExtension("webp")
                .build();
        when(s3Util.toS3UrlByS3Key("images/stored.webp")).thenReturn("https://bucket/images/stored.webp");

        PostImage result = imageUtil.toPostImage(dto, "images/stored.webp");

        assertThat(result.getOriginalName()).isEqualTo("original.png");
        assertThat(result.getStoredName()).isEqualTo("stored.webp");
        assertThat(result.getUrl()).isEqualTo("https://bucket/images/stored.webp");
        assertThat(result.getSize()).isEqualTo(10L);
        assertThat(result.getContentType()).isEqualTo("webp");
    }

    @Test
    @DisplayName("MultipartFile 목록을 PostImage 목록으로 변환한다")
    void toPostImages_returnsPostImages() {
        ImageUtil imageUtil = org.mockito.Mockito.spy(new ImageUtil(s3Util, imageRepository));
        MockMultipartFile file = new MockMultipartFile("images", "image.png", "image/png", "image".getBytes());
        org.mockito.Mockito.doReturn("stored.webp").when(imageUtil).toStoredFilename();
        when(s3Util.buildS3Key("stored.webp")).thenReturn("images/stored.webp");
        when(s3Util.toS3UrlByStoredFilename("stored.webp")).thenReturn("https://bucket/images/stored.webp");

        List<PostImage> result = imageUtil.toPostImages(List.of(file));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getOriginalName()).isEqualTo("image.png");
        assertThat(result.getFirst().getStoredName()).isEqualTo("stored.webp");
        assertThat(result.getFirst().getContentType()).isEqualTo("png");
    }

    @Test
    @DisplayName("빈 MultipartFile 목록은 빈 PostImage 목록을 반환한다")
    void toPostImages_returnsEmptyWhenInputEmpty() {
        ImageUtil imageUtil = new ImageUtil(s3Util, imageRepository);

        assertThat(imageUtil.toPostImages(List.of())).isEmpty();
    }

    @Test
    @DisplayName("저장 파일명은 timestamp와 uuid 형식을 포함한다")
    void toStoredFilename_returnsGeneratedName() {
        ImageUtil imageUtil = new ImageUtil(s3Util, imageRepository);

        assertThat(imageUtil.toStoredFilename()).contains("_");
    }

    @Test
    @DisplayName("raw content type에서 확장자를 추출한다")
    void extractContentTypeFromRawContentType_returnsExtension() {
        ImageUtil imageUtil = new ImageUtil(s3Util, imageRepository);

        assertThat(imageUtil.extractContentTypeFromRawContentType("image/png")).isEqualTo("png");
        assertThatThrownBy(() -> imageUtil.extractContentTypeFromRawContentType(null))
                .isInstanceOf(NullPointerException.class);
    }
}
