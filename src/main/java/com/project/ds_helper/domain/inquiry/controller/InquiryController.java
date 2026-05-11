package com.project.ds_helper.domain.inquiry.controller;

import com.project.ds_helper.common.util.FileUtil;
import com.project.ds_helper.domain.inquiry.dto.request.CreateInquiryReqDto;
import com.project.ds_helper.domain.inquiry.dto.response.GetAllInquiriesOfUserResDto;
import com.project.ds_helper.domain.inquiry.dto.response.GetInquiryResDto;
import com.project.ds_helper.domain.inquiry.service.InquiryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@Slf4j
@RequestMapping("/inquires")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;
    private final FileUtil fileUtil;

    @Tag(name = "문의")
    @Operation(summary = "유저의 전체 문의 조회 (JWT 인증 필요)")
    @GetMapping("/all")
    public ResponseEntity<GetAllInquiriesOfUserResDto> getAllInquiriesOfUser(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "desc") String sort,
            @RequestParam(defaultValue = "createdAt") String sortBy
    ) {
        log.debug("InquiryController.getAllInquiriesOfUser called. page={}, size={}, sort={}, sortBy={}", page, size, sort, sortBy);
        return ResponseEntity.ok(inquiryService.getAllInquiriesOfUser(authentication, page, size, sort, sortBy));
    }

    @Tag(name = "문의")
    @Operation(summary = "단건 문의 조회 (JWT 인증 필요)")
    @GetMapping("/{inquiryId}")
    public ResponseEntity<GetInquiryResDto> getInquiry(
            Authentication authentication,
            @PathVariable("inquiryId") String inquiryId
    ) {
        log.debug("InquiryController.getInquiry called. inquiryId={}", inquiryId);
        return ResponseEntity.ok(inquiryService.getInquiry(authentication, inquiryId));
    }

    @Tag(name = "문의")
    @Operation(summary = "문의 생성 (JWT 인증 필요)")
    @PostMapping(value = "", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ResponseEntity<?> createInquiry(
            Authentication authentication,
            @Parameter(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE))
            @RequestPart(value = "dto") @Valid CreateInquiryReqDto dto,
            @Parameter(content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) throws IOException {
        log.debug("InquiryController.createInquiry called. imageCount={}", images == null ? 0 : images.size());
        inquiryService.createInquiry(authentication, dto, fileUtil.checkIfListIsNull(images));
        return new ResponseEntity<>(null, HttpStatus.CREATED);
    }
}
