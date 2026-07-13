package com.project.ds_helper.domain.trashbin.service;

import com.project.ds_helper.common.dto.request.S3ImageUploadRequestDto;
import com.project.ds_helper.common.enums.ErrorCode;
import com.project.ds_helper.common.exception.BusinessException;
import com.project.ds_helper.domain.post.util.ImageCompressionUtil;
import com.project.ds_helper.domain.post.util.ImageUtil;
import com.project.ds_helper.domain.post.util.S3Util;
import com.project.ds_helper.domain.trashbin.dto.response.UploadTrashBinImageResponseDto;
import com.project.ds_helper.domain.trashbin.dto.response.UploadTrashBinImagesResponseDto;
import com.project.ds_helper.domain.trashbin.entity.TrashBin;
import com.project.ds_helper.domain.trashbin.entity.TrashBinImage;
import com.project.ds_helper.domain.trashbin.repository.TrashBinImageRepository;
import com.project.ds_helper.domain.trashbin.repository.TrashBinRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class TrashBinImageService {

    private final TrashBinRepository trashBinRepository;
    private final TrashBinImageRepository trashBinImageRepository;
    private final ImageUtil imageUtil;
    private final ImageCompressionUtil imageCompressionUtil;
    private final S3Util s3Util;

    public UploadTrashBinImageResponseDto uploadTrashBinImage(MultipartFile image) throws IOException {
        if (image == null || image.isEmpty()) {
            throw invalidParameter("image is required");
        }

        Double latitude = parseLatitudeFromFilename(image.getOriginalFilename());
        TrashBin trashBin = findTrashBinByLatitude(latitude);

        UploadTrashBinImageResponseDto duplicateResponse = trashBinImageRepository.findFirstByTrashBin_Id(trashBin.getId())
                .map(UploadTrashBinImageResponseDto::alreadyExists)
                .orElse(null);
        if (duplicateResponse != null) {
            return duplicateResponse;
        }

        String storedFilename = imageUtil.toStoredFilename();
        S3ImageUploadRequestDto uploadRequestDto = imageCompressionUtil.compressImage(image, storedFilename);
        boolean uploadedToS3 = false;

        try {
            s3Util.uploadImage(uploadRequestDto);
            uploadedToS3 = true;

            String s3Key = s3Util.buildS3Key(storedFilename);
            String imageUrl = s3Util.toS3UrlByS3Key(s3Key);
            TrashBinImage savedImage = trashBinImageRepository.save(TrashBinImage.builder()
                    .originalName(uploadRequestDto.getOriginalFilename())
                    .storedName(uploadRequestDto.getStoredFilename())
                    .s3Key(s3Key)
                    .url(imageUrl)
                    .size(uploadRequestDto.getSize())
                    .contentType(uploadRequestDto.getS3ContentType())
                    .trashBin(trashBin)
                    .build());

            trashBin.updatePhotoUrl(imageUrl);
            return UploadTrashBinImageResponseDto.uploaded(trashBin, savedImage);
        } catch (Exception e) {
            if (uploadedToS3) {
                rollbackUploadedImage(storedFilename);
            }
            throw e;
        }
    }

    public UploadTrashBinImagesResponseDto uploadTrashBinImages(List<MultipartFile> images) throws IOException {
        if (images == null || images.isEmpty()) {
            throw invalidParameter("images are required");
        }

        List<UploadTrashBinImageResponseDto> uploadedImages = new ArrayList<>(images.size());
        try {
            for (MultipartFile image : images) {
                uploadedImages.add(uploadTrashBinImage(image));
            }
            return UploadTrashBinImagesResponseDto.from(uploadedImages);
        } catch (Exception e) {
            rollbackUploadedImages(uploadedImages);
            throw e;
        }
    }

    private TrashBin findTrashBinByLatitude(Double latitude) {
        List<TrashBin> trashBins = trashBinRepository.findAllByLatitude(latitude);
        if (trashBins.isEmpty()) {
            throw invalidParameter("No trash bin found by latitude. latitude=" + latitude);
        }
        if (trashBins.size() > 1) {
            throw conflict("Multiple trash bins found by latitude. Filename latitude must be unique. latitude=" + latitude);
        }
        return trashBins.getFirst();
    }

    private Double parseLatitudeFromFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw invalidParameter("image filename is required");
        }
        String filename = originalFilename.trim();
        int extensionIndex = filename.lastIndexOf('.');
        String latitudeText = extensionIndex > 0 ? filename.substring(0, extensionIndex) : filename;
        try {
            return Double.parseDouble(latitudeText);
        } catch (NumberFormatException e) {
            throw invalidParameter("image filename must be latitude value. originalFilename=" + originalFilename);
        }
    }

    private void rollbackUploadedImage(String storedFilename) {
        try {
            s3Util.deleteImage(storedFilename);
        } catch (Exception rollbackException) {
            log.error("TrashBinImageService.rollbackUploadedImage failed. storedFilename={}", storedFilename, rollbackException);
        }
    }

    private void rollbackUploadedImages(List<UploadTrashBinImageResponseDto> uploadedImages) {
        for (UploadTrashBinImageResponseDto uploadedImage : uploadedImages) {
            if (!uploadedImage.alreadyExists()) {
                rollbackUploadedImage(uploadedImage.storedName());
            }
        }
    }

    private BusinessException invalidParameter(String message) {
        return new BusinessException(ErrorCode.INVALID_PARAMETER, message);
    }

    private BusinessException conflict(String message) {
        return new BusinessException(ErrorCode.CONFLICT, message);
    }
}
