package com.project.ds_helper.domain.inquiry.service;

import com.project.ds_helper.common.dto.request.S3ImageUploadRequestDto;
import com.project.ds_helper.common.util.FileUtil;
import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.inquiry.dto.request.CreateInquiryReqDto;
import com.project.ds_helper.domain.inquiry.entity.Inquiry;
import com.project.ds_helper.domain.inquiry.entity.InquiryImage;
import com.project.ds_helper.domain.inquiry.repository.InquiryImageRepository;
import com.project.ds_helper.domain.inquiry.repository.InquiryRepository;
import com.project.ds_helper.domain.inquiry.util.InquiryImageUtil;
import com.project.ds_helper.domain.post.util.ImageCompressionUtil;
import com.project.ds_helper.domain.post.util.S3Util;
import com.project.ds_helper.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class InquiryCommandService {

    private final InquiryRepository inquiryRepository;
    private final InquiryImageRepository inquiryImageRepository;
    private final UserUtil userUtil;
    private final S3Util s3Util;
    private final InquiryImageUtil inquiryImageUtil;
    private final ImageCompressionUtil imageCompressionUtil;
    private final FileUtil fileUtil;

    @Transactional
    public void createInquiry(Authentication authentication, CreateInquiryReqDto dto, List<MultipartFile> images) throws IOException {
        if (images != null && !images.isEmpty()) {
            fileUtil.checkIfListSizeNotBiggerThanOne(images);
        }

        User user = userUtil.findUserById(userUtil.extractUserId(authentication));
        Inquiry inquiry = dto.toInquiry(dto, user);

        if (images == null || images.isEmpty()) {
            inquiryRepository.save(inquiry);
            return;
        }

        List<InquiryImage> inquiryImages = new ArrayList<>();
        List<S3ImageUploadRequestDto> s3ImageUploadRequestDtos = new ArrayList<>();
        for (MultipartFile image : images) {
            String storedFilename = inquiryImageUtil.toStoredFilename(image.getOriginalFilename());
            S3ImageUploadRequestDto compressedImage = imageCompressionUtil.compressImage(image, storedFilename);
            inquiryImages.add(inquiryImageUtil.toImage(compressedImage, s3Util.buildS3Key(storedFilename), inquiry));
            s3ImageUploadRequestDtos.add(compressedImage);
        }

        inquiryRepository.save(inquiry);
        inquiryImageRepository.saveAll(inquiryImages);
        s3Util.uploadImages(s3ImageUploadRequestDtos);
    }
}
