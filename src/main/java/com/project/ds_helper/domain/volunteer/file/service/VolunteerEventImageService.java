package com.project.ds_helper.domain.volunteer.file.service;

import com.project.ds_helper.common.dto.request.S3ImageUploadRequestDto;
import com.project.ds_helper.common.enums.ErrorCode;
import com.project.ds_helper.common.exception.BusinessException;
import com.project.ds_helper.domain.post.util.ImageCompressionUtil;
import com.project.ds_helper.domain.post.util.S3Util;
import com.project.ds_helper.domain.volunteer.common.enums.VolunteerFileType;
import com.project.ds_helper.domain.volunteer.file.dto.response.VolunteerEventImageUploadResponse;
import com.project.ds_helper.domain.volunteer.file.entity.VolunteerFile;
import com.project.ds_helper.domain.volunteer.file.repository.VolunteerFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class VolunteerEventImageService {

    private static final long MAX_FILE_SIZE = 20L * 1024 * 1024;
    private static final int MAX_LONG_EDGE = 1920;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png");

    private final ImageCompressionUtil imageCompressionUtil;
    private final S3Util s3Util;
    private final VolunteerFileRepository fileRepository;

    @Transactional
    public VolunteerEventImageUploadResponse upload(String adminId, MultipartFile image) throws IOException {
        validateImage(image);
        ImageDimension dimension = resolveStoredDimension(image);
        String storedFilename = "volunteer-event-" + UUID.randomUUID() + ".webp";
        S3ImageUploadRequestDto compressedImage = imageCompressionUtil.compressImage(image, storedFilename);
        String s3Key = s3Util.buildS3Key(storedFilename);
        boolean uploaded = false;

        try {
            s3Util.uploadImage(compressedImage);
            uploaded = true;

            VolunteerFile savedFile = fileRepository.save(VolunteerFile.builder()
                    .ownerUserId(adminId)
                    .fileType(VolunteerFileType.EVENT_IMAGE)
                    .s3Key(s3Key)
                    .originalFilename(image.getOriginalFilename())
                    .contentType(compressedImage.getS3ContentType())
                    .fileSize(compressedImage.getSize())
                    .width(dimension.width())
                    .height(dimension.height())
                    .privateFile(false)
                    .build());

            log.info("VolunteerEventImageService.upload completed. adminId={}, fileId={}, s3Key={}",
                    adminId, savedFile.getId(), s3Key);
            return new VolunteerEventImageUploadResponse(
                    savedFile.getId(),
                    savedFile.getS3Key(),
                    s3Util.toS3UrlByS3Key(savedFile.getS3Key()),
                    savedFile.getContentType(),
                    savedFile.getWidth(),
                    savedFile.getHeight()
            );
        } catch (IOException | RuntimeException e) {
            if (uploaded) {
                s3Util.deleteImagesByS3Key(List.of(s3Key));
            }
            throw e;
        }
    }

    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "봉사 일정 이미지는 필수입니다.");
        }
        if (image.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "봉사 일정 이미지는 20MB 이하여야 합니다.");
        }
        if (!ALLOWED_EXTENSIONS.contains(extensionOf(image.getOriginalFilename()))) {
            throw new BusinessException(ErrorCode.INVALID_PARAMETER, "봉사 일정 이미지는 jpg, jpeg, png만 업로드할 수 있습니다.");
        }
    }

    private ImageDimension resolveStoredDimension(MultipartFile image) throws IOException {
        try (var inputStream = image.getInputStream()) {
            BufferedImage source = ImageIO.read(inputStream);
            if (source == null) {
                throw new BusinessException(ErrorCode.INVALID_PARAMETER, "유효한 이미지 파일이 아닙니다.");
            }
            int longEdge = Math.max(source.getWidth(), source.getHeight());
            if (longEdge <= MAX_LONG_EDGE) {
                return new ImageDimension(source.getWidth(), source.getHeight());
            }
            double scale = (double) MAX_LONG_EDGE / longEdge;
            return new ImageDimension(
                    Math.max(1, (int) Math.round(source.getWidth() * scale)),
                    Math.max(1, (int) Math.round(source.getHeight() * scale))
            );
        }
    }

    private String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex < 0 ? "" : filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private record ImageDimension(int width, int height) {
    }
}
