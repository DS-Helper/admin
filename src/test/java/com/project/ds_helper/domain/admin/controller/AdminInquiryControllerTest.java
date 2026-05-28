package com.project.ds_helper.domain.admin.controller;

import com.project.ds_helper.domain.admin.service.AdminInquiryService;
import com.project.ds_helper.domain.inquiry.dto.response.GetAllInquiriesOfUserResDto;
import com.project.ds_helper.domain.inquiry.enums.InquiryStatus;
import com.project.ds_helper.domain.inquiry.enums.InquiryType;
import com.project.ds_helper.domain.reply.service.ReplyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminInquiryControllerTest {

    @Mock
    private AdminInquiryService adminInquiryService;

    @Mock
    private ReplyService replyService;

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

    @Test
    @DisplayName("臾몄쓽 ?꾩껜 議고쉶???쒕퉬??寃곌낵瑜?諛섑솚?쒕떎")
    void getInquiries_returnsServiceResult() {
        GetAllInquiriesOfUserResDto responseDto = new GetAllInquiriesOfUserResDto(List.of(), null);
        when(adminInquiryService.getInquiries("inquiry-1", InquiryStatus.UNANSWERED, InquiryType.OTHER, LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 2), 0, 10, "desc", "createdAt"))
                .thenReturn(responseDto);

        ResponseEntity<GetAllInquiriesOfUserResDto> response = adminInquiryController.getInquiries(
                "inquiry-1",
                InquiryStatus.UNANSWERED,
                InquiryType.OTHER,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 2),
                0,
                10,
                "desc",
                "createdAt"
        );

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(responseDto);
    }

    @Test
    @DisplayName("臾몄쓽 ?듬? ?섏젙??ReplyService瑜? 호출한다")
    void updateInquiryReply_callsReplyService() {
        org.springframework.security.core.Authentication authentication = org.mockito.Mockito.mock(org.springframework.security.core.Authentication.class);
        com.project.ds_helper.domain.admin.dto.request.UpdateInquiryReplyReqDto dto =
                org.mockito.Mockito.mock(com.project.ds_helper.domain.admin.dto.request.UpdateInquiryReplyReqDto.class);
        when(dto.getContent()).thenReturn("updated");

        adminInquiryController.updateInquiryReply(authentication, "inquiry-1", dto);

        verify(replyService).createReplyOfInquiry(
                org.mockito.ArgumentMatchers.eq(authentication),
                org.mockito.ArgumentMatchers.any(com.project.ds_helper.domain.reply.dto.request.CreateReplyReqDto.class)
        );
    }
}
