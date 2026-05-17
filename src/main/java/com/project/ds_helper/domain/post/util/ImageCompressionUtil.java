package com.project.ds_helper.domain.post.util;

import com.project.ds_helper.common.dto.request.S3ImageUploadRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Component
@Slf4j
public class ImageCompressionUtil {

    private static final int MAX_LONG_EDGE = 1920;
    private static final float WEBP_QUALITY = 0.8f;
    private final WebpImageWriterProvider webpImageWriterProvider;
    private final WebpImageWriteParamConfigurer webpImageWriteParamConfigurer;

    public ImageCompressionUtil() {
        this(new WebpImageWriterProvider(), new WebpImageWriteParamConfigurer());
    }

    ImageCompressionUtil(WebpImageWriterProvider webpImageWriterProvider,
                         WebpImageWriteParamConfigurer webpImageWriteParamConfigurer) {
        this.webpImageWriterProvider = webpImageWriterProvider;
        this.webpImageWriteParamConfigurer = webpImageWriteParamConfigurer;
    }

    /**
     * 모든 S3 업로드는 여기서 한 번 공통 압축을 거친다.
     * 현재 정책은 포맷과 무관하게 항상 WebP 손실 압축으로 통일한다.
     */
    public S3ImageUploadRequestDto compressImage(MultipartFile multipartFile, String storedFilename) throws IOException {
        log.debug("ImageCompressionUtil.compressImage started. originalFilename={}, storedFilename={}, rawContentType={}, rawSize={}",
                multipartFile == null ? null : multipartFile.getOriginalFilename(),
                storedFilename,
                multipartFile == null ? null : multipartFile.getContentType(),
                multipartFile == null ? null : multipartFile.getSize());

        if (multipartFile == null || multipartFile.isEmpty()) {
            throw new IllegalArgumentException("Image file is empty");
        }

        validateImageContentType(multipartFile.getContentType());

        // 1. 원본 이미지를 읽는다. 읽지 못하면 업로드 가능한 이미지가 아니다.
        BufferedImage sourceImage = readSourceImage(multipartFile);

        // 2. 긴 변 기준으로 리사이즈해 과도한 원본 크기를 먼저 줄인다.
        BufferedImage resizedImage = resizeIfNeeded(sourceImage);

        // 3. WebP 손실 압축으로 재인코딩한다.
        byte[] compressedBytes = writeCompressedBytes(resizedImage);

        S3ImageUploadRequestDto dto = S3ImageUploadRequestDto.builder()
                .storedFilename(storedFilename)
                .originalFilename(multipartFile.getOriginalFilename())
                .bytes(compressedBytes)
                .size((long) compressedBytes.length)
                .s3ContentType("image/webp")
                .fileExtension("webp")
                .build();

        log.debug("ImageCompressionUtil.compressImage completed. originalFilename={}, storedFilename={}, compressedContentType={}, compressedSize={}",
                dto.getOriginalFilename(), dto.getStoredFilename(), dto.getS3ContentType(), dto.getSize());
        return dto;
    }

    private void validateImageContentType(String rawContentType) {
        if (rawContentType == null || rawContentType.isBlank()) {
            throw new IllegalArgumentException("Image content type is missing");
        }

        String normalizedContentType = rawContentType.toLowerCase();
        if (!normalizedContentType.startsWith("image/")) {
            throw new IllegalArgumentException("Unsupported image content type: " + rawContentType);
        }
    }

    private BufferedImage readSourceImage(MultipartFile multipartFile) throws IOException {
        try (InputStream inputStream = multipartFile.getInputStream()) {
            BufferedImage sourceImage = ImageIO.read(inputStream);
            if (sourceImage == null) {
                throw new IllegalArgumentException("Unsupported image binary");
            }
            return sourceImage;
        }
    }

    private BufferedImage resizeIfNeeded(BufferedImage sourceImage) {
        int width = sourceImage.getWidth();
        int height = sourceImage.getHeight();
        int longEdge = Math.max(width, height);

        log.debug("ImageCompressionUtil.resizeIfNeeded started. width={}, height={}, longEdge={}", width, height, longEdge);

        if (longEdge <= MAX_LONG_EDGE) {
            log.debug("ImageCompressionUtil.resizeIfNeeded skipped. resize not required.");
            return copyToArgb(sourceImage);
        }

        double scale = (double) MAX_LONG_EDGE / longEdge;
        int resizedWidth = Math.max(1, (int) Math.round(width * scale));
        int resizedHeight = Math.max(1, (int) Math.round(height * scale));

        BufferedImage resizedImage = new BufferedImage(
                resizedWidth,
                resizedHeight,
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D graphics = resizedImage.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.drawImage(sourceImage, 0, 0, resizedWidth, resizedHeight, null);
        } finally {
            graphics.dispose();
        }

        log.debug("ImageCompressionUtil.resizeIfNeeded completed. resizedWidth={}, resizedHeight={}", resizedWidth, resizedHeight);
        return resizedImage;
    }

    private BufferedImage copyToArgb(BufferedImage sourceImage) {
        BufferedImage convertedImage = new BufferedImage(
                sourceImage.getWidth(),
                sourceImage.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );

        Graphics2D graphics = convertedImage.createGraphics();
        try {
            graphics.drawImage(sourceImage, 0, 0, null);
        } finally {
            graphics.dispose();
        }

        return convertedImage;
    }

    private byte[] writeCompressedBytes(BufferedImage image) throws IOException {
        ImageWriter writer = webpImageWriterProvider.findWebpWriter();
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputStream)) {

            writer.setOutput(imageOutputStream);

            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            webpImageWriteParamConfigurer.configure(writeParam, WEBP_QUALITY);

            writer.write(null, new IIOImage(image, null, null), writeParam);
            imageOutputStream.flush();
            outputStream.flush();

            byte[] bytes = outputStream.toByteArray();
            webpImageWriteParamConfigurer.verifyCompressedBytes(bytes);
            return bytes;
        } finally {
            writer.dispose();
        }
    }
}
