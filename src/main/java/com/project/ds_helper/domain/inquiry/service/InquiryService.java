package com.project.ds_helper.domain.inquiry.service;

import com.project.ds_helper.common.dto.request.S3ImageUploadRequestDto;
import com.project.ds_helper.common.util.FileUtil;
import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.inquiry.dto.request.CreateInquiryReqDto;
import com.project.ds_helper.domain.inquiry.dto.response.GetAllInquiriesOfUserResDto;
import com.project.ds_helper.domain.inquiry.dto.response.GetInquiryResDto;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final InquiryImageRepository inquiryImageRepository;
    private final UserUtil userUtil;
    private final S3Util s3Util;
    private final InquiryImageUtil inquiryImageUtil;
    private final ImageCompressionUtil imageCompressionUtil;
    private final FileUtil fileUtil;

    /**
     * 유저의 전체 문의글 조회
     * 최신순으로 보내주기
     * **/
    public GetAllInquiriesOfUserResDto getAllInquiriesOfUser(Authentication authentication, int page, int size, String sort, String sortBy) {
        log.debug("InquiryService.getAllInquiriesOfUser started. page={}, size={}, sort={}, sortBy={}", page, size, sort, sortBy);

        // pageRequest
        Pageable pageRequest = PageRequest.of(page, Math.min(size, 100), sort.equalsIgnoreCase("desc")? Sort.Direction.DESC : Sort.Direction.ASC, "createdAt");
    
        // 유저의 페이징 처리된 문의글 전체 조회
        return GetAllInquiriesOfUserResDto.toDto(inquiryRepository.findAllByUser_Id(userUtil.extractUserId(authentication), pageRequest));
    }

    /**
     * 단건 문의 조회
     * 작성자가 아니어도 조회 가능
     * **/
    public GetInquiryResDto getInquiry(Authentication authentication, String inquiryId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId).orElseThrow( () -> new IllegalArgumentException("Inquiry Not Found"));
        log.debug("InquiryService.getInquiry completed. inquiryId={}", inquiry.getId());
        return GetInquiryResDto.toDto(inquiry);
    }

    /**
     * 문의 생성
     * 이미지 생성
     * s3 저장
     * inquiry에 url 저장
     * **/
    public void createInquiry(Authentication authentication, CreateInquiryReqDto dto, List<MultipartFile> images) throws IOException {
            
            // 이미지 파일 검증
//            images.stream().peek(image -> {
//                try {
//                    fileUtil.isValidSizeAndExtension(image);
//                } catch (IOException e) {
//                    throw new RuntimeException("File Is InValid");
//                }
//            });
            // Check List Size 2 or less
            if(images != null && !images.isEmpty()){
                fileUtil.checkIfListSizeNotBiggerThanOne(images);
            }
            
            // 유저 조회
            User user = userUtil.findUserById(userUtil.extractUserId(authentication));

            // 문의글 엔티티 빌드
            Inquiry inquiry = dto.toInquiry(dto, user);

            // 이미지 업로드
            if(images != null && !images.isEmpty()) {
        log.debug("InquiryService.createInquiry started. imageCount={}", images.size());


                // 이미지 엔티티 빌드
                // 이미지, 문의글 저장
                inquiryRepository.save(inquiry);
        log.debug("InquiryService.createInquiry inquiry saved.");

                List<InquiryImage> inquiryImages = new ArrayList<>();
                List<S3ImageUploadRequestDto> s3ImageUploadRequestDtos = new ArrayList<>();

                for (MultipartFile image : images) {
                    String storedFilename = inquiryImageUtil.toStoredFilename(image.getOriginalFilename());
                    S3ImageUploadRequestDto compressedImage = imageCompressionUtil.compressImage(image, storedFilename);
                    String s3Key = s3Util.buildS3Key(storedFilename);
                    log.debug("InquiryService.createInquiry image prepared. s3ContentType={}, originalFilename={}, storedFilename={}, s3Key={}",
                            compressedImage.getS3ContentType(), compressedImage.getOriginalFilename(), storedFilename, s3Key);
                    inquiryImages.add(inquiryImageUtil.toImage(compressedImage, s3Key, inquiry));
                    s3ImageUploadRequestDtos.add(compressedImage);
                }

                inquiryImageRepository.saveAll(inquiryImages);
        log.debug("InquiryService.createInquiry inquiry images saved.");
                s3Util.uploadImages(s3ImageUploadRequestDtos);
        log.debug("InquiryService.createInquiry images uploaded to S3.");
//                List<InquiryImage> imagesToSave = inquiryImageUtil.toImages(images, inquiry);
//                inquiryImageRepository.saveAll(imagesToSave);

//                List<String> s3Urls = s3Util.uploadImages(images.stream().map(image -> {
//                    try {
//                        return new S3ImageUploadReqDto(inquiryImageUtil.toStoredFilename(image), image.getContentType(), image.getBytes());
//                    } catch (IOException e) {
//                        throw new RuntimeException("S3 Upload Fail");
//                    }
//                }).toList());

            }else{
                inquiryRepository.save(inquiry);
        log.debug("InquiryService.createInquiry completed.");
            }
    }


}
