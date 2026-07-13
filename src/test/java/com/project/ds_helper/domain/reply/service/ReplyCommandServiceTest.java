package com.project.ds_helper.domain.reply.service;

import com.project.ds_helper.common.util.JwtUtil;
import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.inquiry.entity.Inquiry;
import com.project.ds_helper.domain.inquiry.enums.InquiryStatus;
import com.project.ds_helper.domain.inquiry.repository.InquiryRepository;
import com.project.ds_helper.domain.reply.dto.request.CreateReplyReqDto;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReplyCommandServiceTest {
    @Mock private ReplyRepository replyRepository;
    @Mock private InquiryRepository inquiryRepository;
    @Mock private UserUtil userUtil;
    @Mock private JwtUtil jwtUtil;
    @Mock private Authentication authentication;
    @InjectMocks private ReplyCommandService replyCommandService;

    @Test
    @DisplayName("답변이 없으면 새 답변을 생성하고 문의를 ANSWERED로 바꾼다")
    void createReplyOfInquiry_createsReply() {
        User user = User.builder().id("u-1").build();
        Inquiry inquiry = Inquiry.builder().status(InquiryStatus.UNANSWERED).build();
        CreateReplyReqDto dto = CreateReplyReqDto.builder().inquiryId("i-1").content("답변").build();
        when(authentication.getCredentials()).thenReturn("token");
        when(userUtil.extractUserId(authentication)).thenReturn("u-1");
        when(jwtUtil.getRole("token")).thenReturn("ADMIN");
        when(userUtil.findUserById("u-1")).thenReturn(user);
        when(inquiryRepository.findById("i-1")).thenReturn(Optional.of(inquiry));

        replyCommandService.createReplyOfInquiry(authentication, dto);

        assertThat(inquiry.getStatus()).isEqualTo(InquiryStatus.ANSWERED);
    }

    @Test
    @DisplayName("문의가 없으면 예외를 던진다")
    void createReplyOfInquiry_throwsWhenInquiryMissing() {
        CreateReplyReqDto dto = CreateReplyReqDto.builder().inquiryId("i-1").content("답변").build();
        when(authentication.getCredentials()).thenReturn("token");
        when(userUtil.extractUserId(authentication)).thenReturn("u-1");
        when(jwtUtil.getRole("token")).thenReturn("ADMIN");
        when(userUtil.findUserById("u-1")).thenReturn(User.builder().id("u-1").build());
        when(inquiryRepository.findById("i-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> replyCommandService.createReplyOfInquiry(authentication, dto))
                .isInstanceOf(RuntimeException.class);
    }
}
