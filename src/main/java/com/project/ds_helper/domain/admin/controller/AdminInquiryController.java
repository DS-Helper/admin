package com.project.ds_helper.domain.admin.controller;

import com.project.ds_helper.domain.admin.service.AdminInquiryService;
import com.project.ds_helper.domain.inquiry.dto.response.GetAllInquiriesOfUserResDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequestMapping("/admin/inquires")
@RequiredArgsConstructor
public class AdminInquiryController {

    private final AdminInquiryService adminInquiryService;

    @Tag(name = "관리자 > 문의")
    @Operation(summary = "유저의 미응답 문의 조회 (JWT 인증 필요)")
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
}
