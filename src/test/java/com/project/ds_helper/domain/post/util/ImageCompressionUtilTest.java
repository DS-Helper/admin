package com.project.ds_helper.domain.post.util;

import com.project.ds_helper.common.dto.request.S3ImageUploadRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

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
}
