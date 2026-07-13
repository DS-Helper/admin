package com.project.ds_helper.domain.reply.service;

import com.project.ds_helper.domain.reply.entity.Reply;
import com.project.ds_helper.domain.reply.repository.ReplyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReplyQueryServiceTest {
    @Mock private ReplyRepository replyRepository;
    @Mock private Authentication authentication;
    @InjectMocks private ReplyQueryService replyQueryService;

    @Test
    @DisplayName("답변이 없으면 빈 DTO를 반환한다")
    void getReplyByInquiryId_returnsEmptyDto() {
        when(replyRepository.findByInquiry_Id("i-1")).thenReturn(Optional.empty());

        Object result = replyQueryService.getReplyByInquiryId(authentication, "i-1");

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("답변이 있으면 DTO를 반환한다")
    void getReplyByInquiryId_returnsDto() {
        Reply reply = Reply.builder()
                .content("답변")
                .user(com.project.ds_helper.domain.user.entity.User.builder().id("u-1").build())
                .inquiry(com.project.ds_helper.domain.inquiry.entity.Inquiry.builder().content("문의").user(com.project.ds_helper.domain.user.entity.User.builder().id("u-2").build()).build())
                .build();
        when(replyRepository.findByInquiry_Id("i-1")).thenReturn(Optional.of(reply));

        Object result = replyQueryService.getReplyByInquiryId(authentication, "i-1");

        assertThat(result).isNotNull();
    }
}
