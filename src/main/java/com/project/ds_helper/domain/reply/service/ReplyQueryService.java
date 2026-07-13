package com.project.ds_helper.domain.reply.service;

import com.project.ds_helper.domain.reply.dto.response.GetReplyByInquiryIdResDto;
import com.project.ds_helper.domain.reply.entity.Reply;
import com.project.ds_helper.domain.reply.repository.ReplyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReplyQueryService {

    private final ReplyRepository replyRepository;

    @Transactional(readOnly = true)
    public Object getReplyByInquiryId(Authentication authentication, String inquiryId) {
        Reply reply = replyRepository.findByInquiry_Id(inquiryId).orElse(null);
        if (reply == null) {
            return GetReplyByInquiryIdResDto.toEmptyDto();
        }
        return GetReplyByInquiryIdResDto.toDto(reply);
    }
}
