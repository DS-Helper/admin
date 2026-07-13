package com.project.ds_helper.domain.reply.service;

import com.project.ds_helper.domain.reply.dto.request.CreateReplyReqDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReplyService {

    private final ReplyQueryService replyQueryService;
    private final ReplyCommandService replyCommandService;

    public Object getReplyByInquiryId(Authentication authentication, String inquiryId) {
        return replyQueryService.getReplyByInquiryId(authentication, inquiryId);
    }

    public void createReplyOfInquiry(Authentication authentication, CreateReplyReqDto dto) {
        replyCommandService.createReplyOfInquiry(authentication, dto);
    }
}
