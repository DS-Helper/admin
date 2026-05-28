package com.project.ds_helper.domain.reply.service;

import com.project.ds_helper.common.util.JwtUtil;
import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.inquiry.entity.Inquiry;
import com.project.ds_helper.domain.inquiry.enums.InquiryStatus;
import com.project.ds_helper.domain.inquiry.repository.InquiryRepository;
import com.project.ds_helper.domain.reply.dto.request.CreateReplyReqDto;
import com.project.ds_helper.domain.reply.dto.response.GetReplyByInquiryIdResDto;
import com.project.ds_helper.domain.reply.entity.Reply;
import com.project.ds_helper.domain.reply.repository.ReplyRepository;
import com.project.ds_helper.domain.user.entity.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReplyService {

    private final ReplyRepository replyRepository;
    private final InquiryRepository inquiryRepository;
    private final UserUtil userUtil;
    private final JwtUtil jwtUtil;
    
    /**
     * 문의 답변글 조회
     * **/
    public Object getReplyByInquiryId(Authentication authentication, String inquiryId) {
    
        // 답변 조회
        Reply reply = replyRepository.findByInquiry_Id(inquiryId).orElse(null);
        // 답변 글 부재 시 빈 문자열을 채운 dto 응답
        if(reply == null){ return GetReplyByInquiryIdResDto.toEmptyDto();}
        return GetReplyByInquiryIdResDto.toDto(reply);
    }
    
    /**
     * 문의 답변글 생성
     * **/
    public void createReplyOfInquiry(Authentication authentication, CreateReplyReqDto dto) {

        // check args
        String userId = userUtil.extractUserId(authentication);
        String role = jwtUtil.getRole(String.valueOf(authentication.getCredentials()));
        String inquiryId = dto.getInquiryId();
        String content = dto.getContent();
        log.debug("ReplyService.createReplyOfInquiry started. inquiryId={}, role={}, userId={}, contentLength={}", inquiryId, role, userId, content == null ? 0 : content.length());

        // check if reply of inquiry already exist
//        if(replyRepository.findByInquiry_Id(inquiryId).isPresent()){ throw new IllegalArgumentException("Reply Already Exists");}

        // get user, inquiry from db
        User user = userUtil.findUserById(userId);
        Inquiry inquiry = inquiryRepository.findById(inquiryId).orElseThrow( () -> new RuntimeException("Inquiry Not Found"));

        if (inquiry.getReply() != null) {
            Reply existingReply = inquiry.getReply();
            existingReply.setContent(content);
            inquiry.setStatus(InquiryStatus.ANSWERED);
            inquiryRepository.save(inquiry);
            return;
        }

        // build reply
        Reply reply = dto.toReply(dto, user);

        // change Inquiry status and add reply .. automatically save, but just for make sure
        inquiry.setStatus(InquiryStatus.ANSWERED);
        inquiry.addReply(reply);
        inquiryRepository.save(inquiry);
    }
}
