package com.project.ds_helper.domain.inquiry.service;

import com.project.ds_helper.domain.inquiry.dto.request.CreateInquiryReqDto;
import com.project.ds_helper.domain.inquiry.dto.response.GetAllInquiriesOfUserResDto;
import com.project.ds_helper.domain.inquiry.dto.response.GetInquiryResDto;
import com.project.ds_helper.domain.inquiry.enums.InquiryType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InquiryServiceTest {

    @Mock
    private InquiryQueryService inquiryQueryService;

    @Mock
    private InquiryCommandService inquiryCommandService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private InquiryService inquiryService;

    @Test
    @DisplayName("내 문의 목록 조회는 페이지 응답 DTO를 반환한다")
    void getAllInquiriesOfUser_returnsPagedDto() {
        GetAllInquiriesOfUserResDto dto = org.mockito.Mockito.mock(GetAllInquiriesOfUserResDto.class);
        when(inquiryQueryService.getAllInquiriesOfUser(authentication, 0, 10, "desc", "createdAt")).thenReturn(dto);

        GetAllInquiriesOfUserResDto result =
                inquiryService.getAllInquiriesOfUser(authentication, 0, 10, "desc", "createdAt");

        assertThat(result).isEqualTo(dto);
    }

    @Test
    @DisplayName("단건 문의 조회는 Inquiry DTO를 반환한다")
    void getInquiry_returnsDto() {
        GetInquiryResDto dto = org.mockito.Mockito.mock(GetInquiryResDto.class);
        when(inquiryQueryService.getInquiry("inquiry-1")).thenReturn(dto);

        GetInquiryResDto result = inquiryService.getInquiry(authentication, "inquiry-1");

        assertThat(result).isEqualTo(dto);
    }

    @Test
    @DisplayName("이미지가 없는 문의 생성은 문의만 저장한다")
    void createInquiry_savesInquiryWithoutImages() throws Exception {
        CreateInquiryReqDto dto = CreateInquiryReqDto.builder()
                .type(InquiryType.OTHER.getKorean())
                .content("문의 내용")
                .build();
        inquiryService.createInquiry(authentication, dto, List.of());
        org.mockito.Mockito.verify(inquiryCommandService).createInquiry(authentication, dto, List.of());
    }

    @Test
    @DisplayName("이미지 1개가 있는 문의 생성은 저장과 업로드를 수행한다")
    void createInquiry_savesInquiryWithOneImage() throws Exception {
        CreateInquiryReqDto dto = CreateInquiryReqDto.builder()
                .type(InquiryType.OTHER.getKorean())
                .content("문의 내용")
                .build();
        MockMultipartFile image = new MockMultipartFile("images", "a.png", "image/png", "a".getBytes());

        inquiryService.createInquiry(authentication, dto, List.of(image));

        org.mockito.Mockito.verify(inquiryCommandService).createInquiry(authentication, dto, List.of(image));
    }

    @Test
    @DisplayName("이미지 2개가 있는 문의 생성은 예외가 발생한다")
    void createInquiry_throwsWhenImagesTooMany() throws Exception {
        CreateInquiryReqDto dto = CreateInquiryReqDto.builder()
                .type(InquiryType.OTHER.getKorean())
                .content("문의 내용")
                .build();
        MockMultipartFile image1 = new MockMultipartFile("images", "a.png", "image/png", "a".getBytes());
        MockMultipartFile image2 = new MockMultipartFile("images", "b.png", "image/png", "b".getBytes());

        org.mockito.Mockito.doThrow(new IllegalArgumentException("File Size Invalid"))
                .when(inquiryCommandService).createInquiry(authentication, dto, List.of(image1, image2));

        assertThatThrownBy(() -> inquiryService.createInquiry(authentication, dto, List.of(image1, image2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("File Size Invalid");
    }

    @Test
    @DisplayName("문의 서비스는 하위 서비스로 그대로 위임한다")
    void delegatesToSubServices() throws Exception {
        CreateInquiryReqDto dto = CreateInquiryReqDto.builder().type(InquiryType.OTHER.getKorean()).content("문의 내용").build();
        when(inquiryQueryService.getAllInquiriesOfUser(authentication, 0, 10, "desc", "createdAt")).thenReturn(org.mockito.Mockito.mock(GetAllInquiriesOfUserResDto.class));
        when(inquiryQueryService.getInquiry("inquiry-1")).thenReturn(org.mockito.Mockito.mock(GetInquiryResDto.class));

        inquiryService.getAllInquiriesOfUser(authentication, 0, 10, "desc", "createdAt");
        inquiryService.getInquiry(authentication, "inquiry-1");
        inquiryService.createInquiry(authentication, dto, List.of());

        verify(inquiryQueryService).getAllInquiriesOfUser(authentication, 0, 10, "desc", "createdAt");
        verify(inquiryQueryService).getInquiry("inquiry-1");
        verify(inquiryCommandService).createInquiry(authentication, dto, List.of());
    }
}
