package com.project.ds_helper.domain.reply.service;

import com.project.ds_helper.domain.reply.dto.request.CreateReplyReqDto;
import com.project.ds_helper.domain.reply.dto.response.GetReplyByInquiryIdResDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReplyServiceTest {

    @Mock
    private ReplyQueryService replyQueryService;

    @Mock
    private ReplyCommandService replyCommandService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ReplyService replyService;

    @Test
    @DisplayName("답변이 없으면 빈 DTO를 반환한다")
    void getReplyByInquiryId_returnsEmptyDtoWhenMissing() {
        Object dto = GetReplyByInquiryIdResDto.toEmptyDto();
        when(replyQueryService.getReplyByInquiryId(authentication, "inquiry-1")).thenReturn(dto);

        GetReplyByInquiryIdResDto result = (GetReplyByInquiryIdResDto) replyService.getReplyByInquiryId(authentication, "inquiry-1");

        assertThat(result).isEqualTo(dto);
    }

    @Test
    @DisplayName("답변이 있으면 답변 DTO를 반환한다")
    void getReplyByInquiryId_returnsDtoWhenExists() {
        GetReplyByInquiryIdResDto dto = GetReplyByInquiryIdResDto.builder().replyId("reply-1").inquiryId("inquiry-1").content("답변").build();
        when(replyQueryService.getReplyByInquiryId(authentication, "inquiry-1")).thenReturn(dto);

        GetReplyByInquiryIdResDto result = (GetReplyByInquiryIdResDto) replyService.getReplyByInquiryId(authentication, "inquiry-1");

        assertThat(result).isEqualTo(dto);
    }

    @Test
    @DisplayName("문의 답변 생성 시 문의 상태를 ANSWERED로 변경한다")
    void createReplyOfInquiry_updatesInquiryStatus() {
        CreateReplyReqDto dto = CreateReplyReqDto.builder().inquiryId("inquiry-1").content("답변").build();

        replyService.createReplyOfInquiry(authentication, dto);
        org.mockito.Mockito.verify(replyCommandService).createReplyOfInquiry(authentication, dto);
    }

    @Test
    @DisplayName("이미 답변된 문의는 다시 답변할 수 없다")
    void createReplyOfInquiry_updatesWhenAlreadyAnswered() {
        CreateReplyReqDto dto = CreateReplyReqDto.builder().inquiryId("inquiry-1").content("답변").build();

        replyService.createReplyOfInquiry(authentication, dto);
        org.mockito.Mockito.verify(replyCommandService).createReplyOfInquiry(authentication, dto);
    }

    @Test
    @DisplayName("답변 서비스는 하위 서비스로 그대로 위임한다")
    void delegatesToSubServices() {
        GetReplyByInquiryIdResDto dto = GetReplyByInquiryIdResDto.builder().replyId("reply-1").inquiryId("inquiry-1").content("답변").build();
        CreateReplyReqDto createDto = CreateReplyReqDto.builder().inquiryId("inquiry-1").content("답변").build();
        when(replyQueryService.getReplyByInquiryId(authentication, "inquiry-1")).thenReturn(dto);

        replyService.getReplyByInquiryId(authentication, "inquiry-1");
        replyService.createReplyOfInquiry(authentication, createDto);

        verify(replyQueryService).getReplyByInquiryId(authentication, "inquiry-1");
        verify(replyCommandService).createReplyOfInquiry(authentication, createDto);
    }
}
