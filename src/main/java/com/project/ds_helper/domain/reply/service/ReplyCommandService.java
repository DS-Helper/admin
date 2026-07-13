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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReplyCommandService {

    private final ReplyRepository replyRepository;
    private final InquiryRepository inquiryRepository;
    private final UserUtil userUtil;
    private final JwtUtil jwtUtil;

    @Transactional
    public void createReplyOfInquiry(Authentication authentication, CreateReplyReqDto dto) {
        String userId = userUtil.extractUserId(authentication);
        String role = jwtUtil.getRole(String.valueOf(authentication.getCredentials()));
        String inquiryId = dto.getInquiryId();
        String content = dto.getContent();
        log.debug("ReplyCommandService.createReplyOfInquiry started. inquiryId={}, role={}, userId={}, contentLength={}", inquiryId, role, userId, content == null ? 0 : content.length());

        User user = userUtil.findUserById(userId);
        Inquiry inquiry = inquiryRepository.findById(inquiryId).orElseThrow(() -> new RuntimeException("Inquiry Not Found"));

        if (inquiry.getReply() != null) {
            Reply existingReply = inquiry.getReply();
            existingReply.setContent(content);
            inquiry.setStatus(InquiryStatus.ANSWERED);
            inquiryRepository.save(inquiry);
            return;
        }

        Reply reply = dto.toReply(dto, user);
        inquiry.setStatus(InquiryStatus.ANSWERED);
        inquiry.addReply(reply);
        inquiryRepository.save(inquiry);
    }
}
