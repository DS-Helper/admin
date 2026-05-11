package com.project.ds_helper.domain.inquiry.controller;

import com.project.ds_helper.common.util.FileUtil;
import com.project.ds_helper.domain.inquiry.dto.request.CreateInquiryReqDto;
import com.project.ds_helper.domain.inquiry.dto.response.GetAllInquiriesOfUserResDto;
import com.project.ds_helper.domain.inquiry.dto.response.GetInquiryResDto;
import com.project.ds_helper.domain.inquiry.service.InquiryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InquiryControllerTest {

    @Mock
    private InquiryService inquiryService;

    @Mock
    private FileUtil fileUtil;

    @InjectMocks
    private InquiryController inquiryController;

    @Test
    @DisplayName("문의 목록 조회는 서비스 결과를 그대로 반환한다")
    void getAllInquiriesOfUser_returnsServiceResult() {
        GetAllInquiriesOfUserResDto responseDto = new GetAllInquiriesOfUserResDto(List.of(), null);
        when(inquiryService.getAllInquiriesOfUser(null, 0, 10, "desc", "createdAt")).thenReturn(responseDto);

        ResponseEntity<GetAllInquiriesOfUserResDto> response =
                inquiryController.getAllInquiriesOfUser(null, 0, 10, "desc", "createdAt");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(responseDto);
    }

    @Test
    @DisplayName("단건 문의 조회는 서비스 결과를 그대로 반환한다")
    void getInquiry_returnsServiceResult() {
        GetInquiryResDto responseDto = GetInquiryResDto.builder().inquiryId("inquiry-1").content("내용").build();
        when(inquiryService.getInquiry(null, "inquiry-1")).thenReturn(responseDto);

        ResponseEntity<GetInquiryResDto> response = inquiryController.getInquiry(null, "inquiry-1");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(responseDto);
    }

    @Test
    @DisplayName("문의 생성 시 FileUtil로 이미지 null 처리를 거쳐 서비스에 위임한다")
    void createInquiry_delegatesWithNormalizedImages() throws Exception {
        CreateInquiryReqDto dto = CreateInquiryReqDto.builder().type("기타").content("문의").build();
        List<MultipartFile> normalized = List.of();
        when(fileUtil.checkIfListIsNull(null)).thenReturn(normalized);

        ResponseEntity<?> response = inquiryController.createInquiry(null, dto, null);

        verify(fileUtil).checkIfListIsNull(null);
        verify(inquiryService).createInquiry(null, dto, normalized);
        assertThat(response.getStatusCode().value()).isEqualTo(201);
    }
}
