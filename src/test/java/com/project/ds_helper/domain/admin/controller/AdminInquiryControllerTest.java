package com.project.ds_helper.domain.admin.controller;

import com.project.ds_helper.domain.admin.service.AdminInquiryService;
import com.project.ds_helper.domain.inquiry.dto.response.GetAllInquiriesOfUserResDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminInquiryControllerTest {

    @Mock
    private AdminInquiryService adminInquiryService;

    @InjectMocks
    private AdminInquiryController adminInquiryController;

    @Test
    @DisplayName("미답변 문의 목록 조회는 서비스 결과를 반환한다")
    void getAllUnRepliedInquiriesOfUser_returnsServiceResult() {
        GetAllInquiriesOfUserResDto responseDto = new GetAllInquiriesOfUserResDto(List.of(), null);
        when(adminInquiryService.getAllUnRepliedInquiriesOfUser(0, 10, "desc", "createdAt")).thenReturn(responseDto);

        ResponseEntity<GetAllInquiriesOfUserResDto> response =
                adminInquiryController.getAllUnRepliedInquiriesOfUser(0, 10, "desc", "createdAt");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(responseDto);
    }
}
