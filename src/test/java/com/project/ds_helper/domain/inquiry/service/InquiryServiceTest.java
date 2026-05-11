package com.project.ds_helper.domain.inquiry.service;

import com.project.ds_helper.common.util.FileUtil;
import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.inquiry.dto.request.CreateInquiryReqDto;
import com.project.ds_helper.domain.inquiry.dto.response.GetAllInquiriesOfUserResDto;
import com.project.ds_helper.domain.inquiry.dto.response.GetInquiryResDto;
import com.project.ds_helper.domain.inquiry.entity.Inquiry;
import com.project.ds_helper.domain.inquiry.entity.InquiryImage;
import com.project.ds_helper.domain.inquiry.enums.InquiryType;
import com.project.ds_helper.domain.inquiry.repository.InquiryImageRepository;
import com.project.ds_helper.domain.inquiry.repository.InquiryRepository;
import com.project.ds_helper.domain.inquiry.util.InquiryImageUtil;
import com.project.ds_helper.domain.post.util.ImageCompressionUtil;
import com.project.ds_helper.domain.post.util.S3Util;
import com.project.ds_helper.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InquiryServiceTest {

    @Mock
    private InquiryRepository inquiryRepository;

    @Mock
    private InquiryImageRepository inquiryImageRepository;

    @Mock
    private UserUtil userUtil;

    @Mock
    private S3Util s3Util;

    @Mock
    private InquiryImageUtil inquiryImageUtil;

    @Mock
    private ImageCompressionUtil imageCompressionUtil;

    @Mock
    private FileUtil fileUtil;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private InquiryService inquiryService;

    @Test
    @DisplayName("내 문의 목록 조회는 페이지 응답 DTO를 반환한다")
    void getAllInquiriesOfUser_returnsPagedDto() {
        User user = user("user-1");
        Inquiry inquiry = inquiry("inquiry-1", user);
        Page<Inquiry> page = new PageImpl<>(List.of(inquiry));

        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(inquiryRepository.findAllByUser_Id(eq("user-1"), any(Pageable.class))).thenReturn(page);

        GetAllInquiriesOfUserResDto result =
                inquiryService.getAllInquiriesOfUser(authentication, 0, 10, "desc", "createdAt");

        assertThat(result.inquiries()).hasSize(1);
        assertThat(result.inquiries().getFirst().inquiryId()).isEqualTo("inquiry-1");
    }

    @Test
    @DisplayName("단건 문의 조회는 Inquiry DTO를 반환한다")
    void getInquiry_returnsDto() {
        Inquiry inquiry = inquiry("inquiry-1", user("user-1"));
        when(inquiryRepository.findById("inquiry-1")).thenReturn(Optional.of(inquiry));

        GetInquiryResDto result = inquiryService.getInquiry(authentication, "inquiry-1");

        assertThat(result.getInquiryId()).isEqualTo("inquiry-1");
    }

    @Test
    @DisplayName("이미지가 없는 문의 생성은 문의만 저장한다")
    void createInquiry_savesInquiryWithoutImages() throws Exception {
        User user = user("user-1");
        CreateInquiryReqDto dto = CreateInquiryReqDto.builder()
                .type(InquiryType.OTHER.getKorean())
                .content("문의 내용")
                .build();

        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(userUtil.findUserById("user-1")).thenReturn(user);

        inquiryService.createInquiry(authentication, dto, List.of());

        verify(inquiryRepository).save(any(Inquiry.class));
    }

    @Test
    @DisplayName("이미지 1개가 있는 문의 생성은 저장과 업로드를 수행한다")
    void createInquiry_savesInquiryWithOneImage() throws Exception {
        User user = user("user-1");
        CreateInquiryReqDto dto = CreateInquiryReqDto.builder()
                .type(InquiryType.OTHER.getKorean())
                .content("문의 내용")
                .build();
        MockMultipartFile image = new MockMultipartFile("images", "a.png", "image/png", "a".getBytes());
        InquiryImage inquiryImage = org.mockito.Mockito.mock(InquiryImage.class);

        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(userUtil.findUserById("user-1")).thenReturn(user);
        when(inquiryImageUtil.toStoredFilename("a.png")).thenReturn("stored-a.png");
        when(s3Util.buildS3Key("stored-a.png")).thenReturn("inquiry/stored-a.png");
        when(inquiryImageUtil.toImage(any(), any(), any())).thenReturn(inquiryImage);
        when(imageCompressionUtil.compressImage(image, "stored-a.png"))
                .thenReturn(compressedImage("stored-a.png", "a.png", "png", "a".getBytes()));

        inquiryService.createInquiry(authentication, dto, List.of(image));

        verify(fileUtil).checkIfListSizeNotBiggerThanOne(List.of(image));
        verify(inquiryRepository).save(any(Inquiry.class));
        verify(inquiryImageRepository).saveAll(anyList());
        verify(s3Util).uploadImages(anyList());
    }

    @Test
    @DisplayName("이미지 2개가 있는 문의 생성은 예외가 발생한다")
    void createInquiry_throwsWhenImagesTooMany() {
        CreateInquiryReqDto dto = CreateInquiryReqDto.builder()
                .type(InquiryType.OTHER.getKorean())
                .content("문의 내용")
                .build();
        MockMultipartFile image1 = new MockMultipartFile("images", "a.png", "image/png", "a".getBytes());
        MockMultipartFile image2 = new MockMultipartFile("images", "b.png", "image/png", "b".getBytes());

        org.mockito.Mockito.doThrow(new IllegalArgumentException("File Size Invalid"))
                .when(fileUtil).checkIfListSizeNotBiggerThanOne(anyList());

        assertThatThrownBy(() -> inquiryService.createInquiry(authentication, dto, List.of(image1, image2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File Size Invalid");
    }

    private User user(String id) {
        return User.builder().id(id).name("tester").build();
    }

    private Inquiry inquiry(String id, User user) {
        return Inquiry.builder()
                .id(id)
                .content("문의")
                .type(InquiryType.OTHER)
                .user(user)
                .build();
    }

    private com.project.ds_helper.common.dto.request.S3ImageUploadRequestDto compressedImage(
            String storedFilename,
            String originalFilename,
            String fileExtension,
            byte[] bytes
    ) {
        return com.project.ds_helper.common.dto.request.S3ImageUploadRequestDto.builder()
                .storedFilename(storedFilename)
                .originalFilename(originalFilename)
                .bytes(bytes)
                .size((long) bytes.length)
                .s3ContentType("image/" + fileExtension)
                .fileExtension(fileExtension)
                .build();
    }
}
