package com.project.ds_helper.domain.reply.controller;

import com.project.ds_helper.domain.reply.dto.request.CreateReplyReqDto;
import com.project.ds_helper.domain.reply.dto.response.GetReplyByInquiryIdResDto;
import com.project.ds_helper.domain.reply.service.ReplyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReplyControllerTest {

    @Mock
    private ReplyService replyService;

    @InjectMocks
    private ReplyController replyController;

    @Test
    @DisplayName("문의 답변 조회는 서비스 결과를 그대로 반환한다")
    void getReplyByInquiryId_returnsServiceResult() {
        GetReplyByInquiryIdResDto responseDto = new GetReplyByInquiryIdResDto("reply-1", "답변", "inquiry-1");
        when(replyService.getReplyByInquiryId(null, "inquiry-1")).thenReturn(responseDto);

        ResponseEntity<?> response = replyController.getReplyByInquiryId(null, "inquiry-1");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(responseDto);
    }

    @Test
    @DisplayName("문의 답변 생성은 서비스 호출 후 200을 반환한다")
    void createReplyOfInquiry_returnsOk() {
        CreateReplyReqDto dto = CreateReplyReqDto.builder().inquiryId("inquiry-1").content("답변").build();

        ResponseEntity<?> response = replyController.createReplyOfInquiry(null, dto);

        verify(replyService).createReplyOfInquiry(null, dto);
        assertThat(response.getStatusCode().value()).isEqualTo(200);
    }
}
