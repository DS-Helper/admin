package com.project.ds_helper.domain.reply.service;

import com.project.ds_helper.common.util.JwtUtil;
import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.inquiry.entity.Inquiry;
import com.project.ds_helper.domain.inquiry.enums.InquiryStatus;
import com.project.ds_helper.domain.inquiry.enums.InquiryType;
import com.project.ds_helper.domain.inquiry.repository.InquiryRepository;
import com.project.ds_helper.domain.reply.dto.request.CreateReplyReqDto;
import com.project.ds_helper.domain.reply.dto.response.GetReplyByInquiryIdResDto;
import com.project.ds_helper.domain.reply.entity.Reply;
import com.project.ds_helper.domain.reply.repository.ReplyRepository;
import com.project.ds_helper.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReplyServiceTest {

    @Mock
    private ReplyRepository replyRepository;

    @Mock
    private InquiryRepository inquiryRepository;

    @Mock
    private UserUtil userUtil;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ReplyService replyService;

    @Test
    @DisplayName("답변이 없으면 빈 DTO를 반환한다")
    void getReplyByInquiryId_returnsEmptyDtoWhenMissing() {
        when(replyRepository.findByInquiry_Id("inquiry-1")).thenReturn(Optional.empty());

        GetReplyByInquiryIdResDto result = (GetReplyByInquiryIdResDto) replyService.getReplyByInquiryId(authentication, "inquiry-1");

        assertThat(result.replyId()).isEmpty();
        assertThat(result.inquiryId()).isEmpty();
    }

    @Test
    @DisplayName("답변이 있으면 답변 DTO를 반환한다")
    void getReplyByInquiryId_returnsDtoWhenExists() {
        Inquiry inquiry = inquiry("inquiry-1", user("user-1"));
        Reply reply = Reply.builder().id("reply-1").content("답변").inquiry(inquiry).build();

        when(replyRepository.findByInquiry_Id("inquiry-1")).thenReturn(Optional.of(reply));

        GetReplyByInquiryIdResDto result = (GetReplyByInquiryIdResDto) replyService.getReplyByInquiryId(authentication, "inquiry-1");

        assertThat(result.replyId()).isEqualTo("reply-1");
        assertThat(result.inquiryId()).isEqualTo("inquiry-1");
    }

    @Test
    @DisplayName("문의 답변 생성 시 문의 상태를 ANSWERED로 변경한다")
    void createReplyOfInquiry_updatesInquiryStatus() {
        User admin = user("admin-1");
        Inquiry inquiry = inquiry("inquiry-1", admin);
        CreateReplyReqDto dto = CreateReplyReqDto.builder().inquiryId("inquiry-1").content("답변").build();

        when(userUtil.extractUserId(authentication)).thenReturn("admin-1");
        when(jwtUtil.getRole(String.valueOf(authentication.getCredentials()))).thenReturn("ADMIN");
        when(userUtil.findUserById("admin-1")).thenReturn(admin);
        when(inquiryRepository.findById("inquiry-1")).thenReturn(Optional.of(inquiry));

        replyService.createReplyOfInquiry(authentication, dto);

        assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.ANSWERED);
        assertThat(inquiry.getReply()).isNotNull();
        verify(inquiryRepository).save(inquiry);
    }

    @Test
    @DisplayName("이미 답변된 문의는 다시 답변할 수 없다")
    void createReplyOfInquiry_throwsWhenAlreadyAnswered() {
        User admin = user("admin-1");
        Inquiry inquiry = inquiry("inquiry-1", admin);
        inquiry.setReply(Reply.builder().id("reply-1").content("기존 답변").user(admin).build());
        CreateReplyReqDto dto = CreateReplyReqDto.builder().inquiryId("inquiry-1").content("답변").build();

        when(userUtil.extractUserId(authentication)).thenReturn("admin-1");
        when(jwtUtil.getRole(String.valueOf(authentication.getCredentials()))).thenReturn("ADMIN");
        when(userUtil.findUserById("admin-1")).thenReturn(admin);
        when(inquiryRepository.findById("inquiry-1")).thenReturn(Optional.of(inquiry));

        assertThatThrownBy(() -> replyService.createReplyOfInquiry(authentication, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Already Answered");
    }

    private User user(String id) {
        return User.builder().id(id).name("admin").build();
    }

    private Inquiry inquiry(String id, User user) {
        return Inquiry.builder()
                .id(id)
                .content("문의")
                .type(InquiryType.OTHER)
                .status(InquiryStatus.UNANSWERED)
                .user(user)
                .build();
    }
}
