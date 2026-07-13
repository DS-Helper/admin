package com.project.ds_helper.domain.inquiry.service;

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
import com.project.ds_helper.domain.user.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InquiryCommandServiceTest {

    @Mock private InquiryRepository inquiryRepository;
    @Mock private InquiryImageRepository inquiryImageRepository;
    @Mock private UserUtil userUtil;
    @Mock private S3Util s3Util;
    @Mock private InquiryImageUtil inquiryImageUtil;
    @Mock private ImageCompressionUtil imageCompressionUtil;
    @Mock private FileUtil fileUtil;
    @Mock private Authentication authentication;

    @InjectMocks
    private InquiryCommandService inquiryCommandService;

    @Test
    @DisplayName("이미지가 없으면 문의만 저장한다")
    void createInquiry_savesInquiryOnly() throws Exception {
        User user = User.builder().id("u-1").role(UserRole.USER).build();
        CreateInquiryReqDto dto = CreateInquiryReqDto.builder().type("기타").content("내용").build();

        when(userUtil.extractUserId(authentication)).thenReturn("u-1");
        when(userUtil.findUserById("u-1")).thenReturn(user);

        inquiryCommandService.createInquiry(authentication, dto, List.of());

        verify(inquiryRepository).save(any(Inquiry.class));
    }

    @Test
    @DisplayName("이미지가 있으면 문의 이미지와 S3 업로드를 수행한다")
    void createInquiry_savesImages() throws Exception {
        User user = User.builder().id("u-1").role(UserRole.USER).build();
        CreateInquiryReqDto dto = CreateInquiryReqDto.builder().type("기타").content("내용").build();
        MockMultipartFile image = new MockMultipartFile("images", "a.png", "image/png", "a".getBytes());

        when(userUtil.extractUserId(authentication)).thenReturn("u-1");
        when(userUtil.findUserById("u-1")).thenReturn(user);
        when(inquiryImageUtil.toStoredFilename("a.png")).thenReturn("stored");
        when(imageCompressionUtil.compressImage(image, "stored")).thenReturn(
                com.project.ds_helper.common.dto.request.S3ImageUploadRequestDto.builder()
                        .storedFilename("stored")
                        .originalFilename("a.png")
                        .bytes("a".getBytes())
                        .size(1L)
                        .s3ContentType("image/png")
                        .fileExtension("png")
                        .build()
        );
        when(s3Util.buildS3Key("stored")).thenReturn("key");
        when(inquiryImageUtil.toImage(any(), any(), any())).thenReturn(InquiryImage.builder().build());

        inquiryCommandService.createInquiry(authentication, dto, List.of(image));

        verify(inquiryRepository).save(any(Inquiry.class));
        verify(inquiryImageRepository).saveAll(any());
        verify(s3Util).uploadImages(any());
    }

    @Test
    @DisplayName("이미지가 2개면 예외를 던진다")
    void createInquiry_throwsWhenTooManyImages() {
        CreateInquiryReqDto dto = CreateInquiryReqDto.builder().type("기타").content("내용").build();
        MockMultipartFile image1 = new MockMultipartFile("images", "a.png", "image/png", "a".getBytes());
        MockMultipartFile image2 = new MockMultipartFile("images", "b.png", "image/png", "b".getBytes());

        doThrow(new IllegalArgumentException("File Size Invalid")).when(fileUtil).checkIfListSizeNotBiggerThanOne(List.of(image1, image2));

        assertThatThrownBy(() -> inquiryCommandService.createInquiry(authentication, dto, List.of(image1, image2)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
