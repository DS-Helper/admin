package com.project.ds_helper.domain.admin.controller;

import com.project.ds_helper.domain.admin.service.AdminInquiryService;
import com.project.ds_helper.domain.admin.dto.request.UpdateInquiryReplyReqDto;
import com.project.ds_helper.domain.inquiry.dto.response.GetAllInquiriesOfUserResDto;
import com.project.ds_helper.domain.inquiry.enums.InquiryStatus;
import com.project.ds_helper.domain.inquiry.enums.InquiryType;
import com.project.ds_helper.domain.reply.dto.request.CreateReplyReqDto;
import com.project.ds_helper.domain.reply.service.ReplyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.time.LocalDate;

@RestController
@Slf4j
@RequestMapping("/admin/inquires")
@RequiredArgsConstructor
public class AdminInquiryController {

    private final AdminInquiryService adminInquiryService;
    private final ReplyService replyService;

    @Tag(name = "관리자 > 문의")
    @Operation(
            summary = "문의 전체 조회 (JWT 인증 필요)",
            description = ""
                    + "- status가 null이면 UNANSWERED/ANSWERED 전체를 조회합니다.\n"
                    + "- inquiryId는 정확히 일치하는 ID로 필터링합니다.\n"
                    + "- 날짜 필터(startDate/endDate)는 문의 생성일(createdAt) 기준입니다."
    )
    @GetMapping()
    public ResponseEntity<GetAllInquiriesOfUserResDto> getInquiries(
            @RequestParam(required = false) String inquiryId,
            @RequestParam(required = false) InquiryStatus status,
            @RequestParam(required = false) InquiryType type,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "desc") String sort,
            @RequestParam(defaultValue = "createdAt") String sortBy
    ) {
        log.debug("AdminInquiryController.getInquiries called. inquiryId={}, status={}, type={}, dateRange={}~{}",
                inquiryId, status, type, startDate, endDate);
        return ResponseEntity.ok(adminInquiryService.getInquiries(inquiryId, status, type, startDate, endDate, page, size, sort, sortBy));
    }

    @Tag(name = "관리자 > 문의")
    @Operation(
            summary = "유저의 미응답 문의 조회 (JWT 인증 필요)",
            description = "답변 상태가 UNANSWERED(미응답)인 문의만 조회합니다. (기존 API 유지)"
    )
    @GetMapping("/un-replied")
    public ResponseEntity<GetAllInquiriesOfUserResDto> getAllUnRepliedInquiriesOfUser(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "desc") String sort,
            @RequestParam(defaultValue = "createdAt") String sortBy
    ) {
        log.debug("AdminInquiryController.getAllUnRepliedInquiriesOfUser called. page={}, size={}, sort={}, sortBy={}", page, size, sort, sortBy);
        return ResponseEntity.ok(adminInquiryService.getAllUnRepliedInquiriesOfUser(page, size, sort, sortBy));
    }

    @Tag(name = "관리자 > 문의")
    @Operation(
            summary = "문의 답변 수정 (JWT 인증 필요)",
            description = ""
                    + "- 기존 Reply 생성 로직(ReplyService.createReplyOfInquiry)을 재사용합니다.\n"
                    + "- 문의에 답변이 없으면 생성, 답변이 있으면 content를 업데이트합니다."
    )
    @PutMapping("/{inquiryId}/reply")
    public ResponseEntity<?> updateInquiryReply(
            Authentication authentication,
            @PathVariable String inquiryId,
            @RequestBody @Valid UpdateInquiryReplyReqDto dto
    ) {
        log.debug("AdminInquiryController.updateInquiryReply called. inquiryId={}", inquiryId);
        CreateReplyReqDto createDto = new CreateReplyReqDto();
        createDto.setInquiryId(inquiryId);
        createDto.setContent(dto.getContent());
        replyService.createReplyOfInquiry(authentication, createDto);
        return ResponseEntity.ok("");
    }
}
