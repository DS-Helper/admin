package com.project.ds_helper.domain.post.util;

import com.project.ds_helper.common.dto.request.S3ImageUploadRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class ImageCompressionUtilTest {

    private final ImageCompressionUtil imageCompressionUtil = new ImageCompressionUtil();

    @Test
    @DisplayName("모든 이미지는 긴 변 기준 리사이즈 후 WebP 손실 압축으로 변환한다")
    void compressImage_convertsToLossyWebp() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "sample.jpg",
                "image/jpeg",
                createImageBytes(4000, 2000, "jpeg", false)
        );

        S3ImageUploadRequestDto result = imageCompressionUtil.compressImage(image, "stored-file");

        assertThat(result.getStoredFilename()).isEqualTo("stored-file");
        assertThat(result.getOriginalFilename()).isEqualTo("sample.jpg");
        assertThat(result.getS3ContentType()).isEqualTo("image/webp");
        assertThat(result.getFileExtension()).isEqualTo("webp");
        assertThat(result.getSize()).isEqualTo((long) result.getBytes().length);
        assertThat(result.getBytes().length).isGreaterThan(0);
        assertThat(result.getSize()).isLessThan((long) image.getBytes().length);
    }

    @Test
    @DisplayName("PNG도 예외 없이 WebP 손실 압축으로 변환한다")
    void compressImage_convertsPngToLossyWebp() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "sample.png",
                "image/png",
                createImageBytes(1200, 800, "png", true)
        );

        S3ImageUploadRequestDto result = imageCompressionUtil.compressImage(image, "stored-file");

        assertThat(result.getS3ContentType()).isEqualTo("image/webp");
        assertThat(result.getFileExtension()).isEqualTo("webp");
        assertThat(result.getBytes().length).isGreaterThan(0);
    }

    @Test
    @DisplayName("compressImage rejects invalid image inputs")
    void compressImage_rejectsInvalidInputs() {
        assertThatThrownBy(() -> imageCompressionUtil.compressImage(null, "stored-file"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Image file is empty");
        assertThatThrownBy(() -> imageCompressionUtil.compressImage(new MockMultipartFile("image", new byte[0]), "stored-file"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Image file is empty");
        assertThatThrownBy(() -> imageCompressionUtil.compressImage(new MockMultipartFile("image", "sample.png", null, "image".getBytes()), "stored-file"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Image content type is missing");
        assertThatThrownBy(() -> imageCompressionUtil.compressImage(new MockMultipartFile("image", "sample.png", "   ", "image".getBytes()), "stored-file"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Image content type is missing");
        assertThatThrownBy(() -> imageCompressionUtil.compressImage(new MockMultipartFile("image", "sample.txt", "text/plain", "image".getBytes()), "stored-file"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported image content type");
        assertThatThrownBy(() -> imageCompressionUtil.compressImage(new MockMultipartFile("image", "sample.png", "image/png", "not-image".getBytes()), "stored-file"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported image binary");
    }

    @Test
    @DisplayName("compressImage fails when WebP writer is unavailable")
    void compressImage_throwsWhenWebpWriterMissing() throws Exception {
        ImageCompressionUtil util = new ImageCompressionUtil(
                new WebpImageWriterProvider(formatName -> Collections.emptyIterator()),
                new WebpImageWriteParamConfigurer()
        );
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "sample.png",
                "image/png",
                createImageBytes(10, 10, "png", false)
        );

        assertThatThrownBy(() -> util.compressImage(image, "stored-file"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No ImageWriter found for WebP");
    }

    @Test
    @DisplayName("WebpImageWriterProvider returns the first available writer")
    void webpImageWriterProvider_returnsFirstWriter() {
        ImageWriter writer = mock(ImageWriter.class);
        WebpImageWriterProvider provider = new WebpImageWriterProvider(formatName -> {
            if ("WebP".equals(formatName)) {
                return List.of(writer).iterator();
            }
            return Collections.emptyIterator();
        });

        ImageWriter result = provider.findWebpWriter();

        assertThat(result).isSameAs(writer);
    }

    @Test
    @DisplayName("WebpImageWriteParamConfigurer configures compression options")
    void webpImageWriteParamConfigurer_configuresCompression() {
        WebpImageWriteParamConfigurer configurer = new WebpImageWriteParamConfigurer();
        FakeImageWriteParam writeParam = new FakeImageWriteParam(true, new String[]{"lossless", "lossy"});

        configurer.configure(writeParam, 0.8f);

        assertThat(writeParam.compressionMode).isEqualTo(ImageWriteParam.MODE_EXPLICIT);
        assertThat(writeParam.compressionType).isEqualTo("lossy");
        assertThat(writeParam.compressionQuality).isEqualTo(0.8f);
    }

    @Test
    @DisplayName("WebpImageWriteParamConfigurer handles optional compression options")
    void webpImageWriteParamConfigurer_handlesOptionalCompressionOptions() throws Exception {
        WebpImageWriteParamConfigurer configurer = new WebpImageWriteParamConfigurer();
        FakeImageWriteParam notCompressed = new FakeImageWriteParam(false, new String[]{"lossy"});
        FakeImageWriteParam nullTypes = new FakeImageWriteParam(true, null);
        FakeImageWriteParam emptyTypes = new FakeImageWriteParam(true, new String[]{});
        FakeImageWriteParam fallbackTypes = new FakeImageWriteParam(true, new String[]{"LossLess"});

        configurer.configure(notCompressed, 0.8f);
        configurer.configure(nullTypes, 0.8f);
        configurer.configure(emptyTypes, 0.8f);
        configurer.configure(fallbackTypes, 0.8f);

        assertThat(notCompressed.compressionQuality).isNull();
        assertThat(nullTypes.compressionType).isNull();
        assertThat(emptyTypes.compressionType).isNull();
        assertThat(fallbackTypes.compressionType).isEqualTo("LossLess");
        assertThat(configurer.findLossyCompressionType(new String[]{"abc", "LoSsY"})).isEqualTo("LoSsY");
        assertThat(configurer.findLossyCompressionType(new String[]{"abc", "def"})).isEqualTo("abc");
        configurer.verifyCompressedBytes(new byte[]{1});
        assertThatThrownBy(() -> configurer.verifyCompressedBytes(new byte[0]))
                .isInstanceOf(java.io.IOException.class)
                .hasMessageContaining("Compressed image verification failed");
    }

    private byte[] createImageBytes(int width, int height, String formatName, boolean transparent) throws Exception {
        BufferedImage image = new BufferedImage(
                width,
                height,
                transparent ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB
        );

        Graphics2D graphics = image.createGraphics();
        try {
            if (transparent) {
                graphics.setComposite(AlphaComposite.Clear);
                graphics.fillRect(0, 0, width, height);
                graphics.setComposite(AlphaComposite.Src);
            } else {
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, width, height);
            }
            graphics.setColor(Color.BLUE);
            graphics.fillRect(10, 10, width - 20, height - 20);
        } finally {
            graphics.dispose();
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, formatName, outputStream);
        return outputStream.toByteArray();
    }

    private static class FakeImageWriteParam extends ImageWriteParam {
        private final boolean canWriteCompressed;
        private final String[] compressionTypes;
        private Integer compressionMode;
        private String compressionType;
        private Float compressionQuality;

        private FakeImageWriteParam(boolean canWriteCompressed, String[] compressionTypes) {
            this.canWriteCompressed = canWriteCompressed;
            this.compressionTypes = compressionTypes;
        }

        @Override
        public boolean canWriteCompressed() {
            return canWriteCompressed;
        }

        @Override
        public String[] getCompressionTypes() {
            return compressionTypes;
        }

        @Override
        public void setCompressionMode(int mode) {
            this.compressionMode = mode;
        }

        @Override
        public void setCompressionType(String compressionType) {
            this.compressionType = compressionType;
        }

        @Override
        public void setCompressionQuality(float quality) {
            this.compressionQuality = quality;
        }
    }
}
