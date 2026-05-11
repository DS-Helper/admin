package com.project.ds_helper.domain.inquiry.util;

import com.project.ds_helper.common.dto.request.S3ImageUploadRequestDto;
import com.project.ds_helper.domain.inquiry.entity.Inquiry;
import com.project.ds_helper.domain.inquiry.entity.InquiryImage;
import com.project.ds_helper.domain.inquiry.repository.InquiryImageRepository;
import com.project.ds_helper.domain.post.entity.PostImage;
import com.project.ds_helper.domain.post.repository.ImageRepository;
import com.project.ds_helper.domain.post.util.ImageUtil;
import com.project.ds_helper.domain.post.util.S3Util;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
@Slf4j
@RequiredArgsConstructor
public class InquiryImageUtil{

    private final S3Util s3Util;
    private final InquiryImageRepository inquiryImageRepository;
    private final ImageUtil imageUtil;

    /**
     * 이미지 엔티티 빌드
     * **/
    public InquiryImage toImage(S3ImageUploadRequestDto imageUploadRequestDto, String s3Key, Inquiry inquiry){
                    InquiryImage inquiryImage = InquiryImage.builder()
                    .originalName(imageUploadRequestDto.getOriginalFilename())
                    .storedName(imageUploadRequestDto.getStoredFilename())
                    .url(s3Util.toS3UrlByS3Key(s3Key))
                    .size(imageUploadRequestDto.getSize())
                    .contentType(imageUploadRequestDto.getFileExtension())
                    .inquiry(inquiry)
                    .build();
        log.info("InquiryImage Successfully Built");
        return inquiryImage;
    }

    /**
     * 이미지 엔티티 리스트 빌드
     * **/
    public List<InquiryImage> toImages(List<MultipartFile> images, Inquiry inquiry){
        List<InquiryImage> result = new ArrayList<>();
        if(!images.isEmpty()) {
            images.forEach( image -> result.add(InquiryImage.builder()
                    .originalName(image.getOriginalFilename())
                    .storedName(imageUtil.toStoredFilename())
                    .url(s3Util.toS3UrlByStoredFilename(imageUtil.toStoredFilename()))
                    .size(image.getSize())
                    .contentType(Objects.requireNonNull(image.getContentType()).split("/")[1])
                    .inquiry(inquiry)
                    .build()));
        }
        log.info("InquiryImage Successfully Built");
        if(!result.isEmpty()){
            log.info("InquiryImages Size : {}", result.size());
        }else{
            log.info("InquiryImages is Empty : {}", result.size());
        }
        return result;
    }   
    
    
    /**
     * ImageUtil의 toStoredFilename을 리턴함. 종속성 있음 (유지보수성 향상을 위해서, 하지만 역할 분리를 위해 InquiryImageUtil에서 Call해서 사용)
     * **/
    public String toStoredFilename(String originalFilename){
       return imageUtil.toStoredFilename();
    }

    /**
     * ImageUtil의 extractContentType을 리턴함. 종속성 있음 (유지보수성 향상을 위해서, 하지만 역할 분리를 위해 InquiryImageUtil에서 Call해서 사용)
     * **/
    public String extractContentTypeFromRawContentType(String rawContentType){
        return imageUtil.extractContentTypeFromRawContentType(rawContentType);
    }
}
