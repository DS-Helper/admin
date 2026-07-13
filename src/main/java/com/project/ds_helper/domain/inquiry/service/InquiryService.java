package com.project.ds_helper.domain.inquiry.service;

import com.project.ds_helper.domain.inquiry.dto.request.CreateInquiryReqDto;
import com.project.ds_helper.domain.inquiry.dto.response.GetAllInquiriesOfUserResDto;
import com.project.ds_helper.domain.inquiry.dto.response.GetInquiryResDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InquiryService {

    private final InquiryQueryService inquiryQueryService;
    private final InquiryCommandService inquiryCommandService;

    public GetAllInquiriesOfUserResDto getAllInquiriesOfUser(Authentication authentication, int page, int size, String sort, String sortBy) {
        return inquiryQueryService.getAllInquiriesOfUser(authentication, page, size, sort, sortBy);
    }

    public GetInquiryResDto getInquiry(Authentication authentication, String inquiryId) {
        return inquiryQueryService.getInquiry(inquiryId);
    }

    public void createInquiry(Authentication authentication, CreateInquiryReqDto dto, List<MultipartFile> images) throws IOException {
        inquiryCommandService.createInquiry(authentication, dto, images);
    }
}
