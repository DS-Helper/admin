package com.project.ds_helper.domain.reply.controller;

import com.project.ds_helper.common.enums.SwaggerTagName;
import com.project.ds_helper.domain.reply.dto.request.CreateReplyReqDto;
import com.project.ds_helper.domain.reply.service.ReplyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/replies")
public class ReplyController {

    private final ReplyService replyService;

    @Tag(name = SwaggerTagName.INQUIRY_REPLY)
    @Operation(summary = "문의 답변 단건 조회 (JWT 인증 필요)")
    @GetMapping("/{inquiryId}")
    public ResponseEntity<?> getReplyByInquiryId(
            Authentication authentication,
            @PathVariable(name = "inquiryId") String inquiryId
    ) {
        log.debug("ReplyController.getReplyByInquiryId called. inquiryId={}", inquiryId);
        return ResponseEntity.ok(replyService.getReplyByInquiryId(authentication, inquiryId));
    }

    @Tag(name = SwaggerTagName.INQUIRY_REPLY)
    @Operation(summary = "문의 답변 생성 (JWT 인증 필요)")
    @PostMapping()
    public ResponseEntity<?> createReplyOfInquiry(
            Authentication authentication,
            @RequestBody @Valid CreateReplyReqDto dto
    ) {
        log.debug("ReplyController.createReplyOfInquiry called. inquiryId={}", dto.getInquiryId());
        replyService.createReplyOfInquiry(authentication, dto);
        return ResponseEntity.ok("");
    }
}
