package com.project.ds_helper.domain.inquiry.service;

import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.inquiry.dto.response.GetAllInquiriesOfUserResDto;
import com.project.ds_helper.domain.inquiry.dto.response.GetInquiryResDto;
import com.project.ds_helper.domain.inquiry.entity.Inquiry;
import com.project.ds_helper.domain.inquiry.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InquiryQueryService {

    private final InquiryRepository inquiryRepository;
    private final UserUtil userUtil;

    @Transactional(readOnly = true)
    public GetAllInquiriesOfUserResDto getAllInquiriesOfUser(Authentication authentication, int page, int size, String sort, String sortBy) {
        Pageable pageRequest = PageRequest.of(page, Math.min(size, 100), sort.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC, "createdAt");
        return GetAllInquiriesOfUserResDto.toDto(inquiryRepository.findAllByUser_Id(userUtil.extractUserId(authentication), pageRequest));
    }

    @Transactional(readOnly = true)
    public GetInquiryResDto getInquiry(String inquiryId) {
        Inquiry inquiry = inquiryRepository.findById(inquiryId).orElseThrow(() -> new IllegalArgumentException("Inquiry Not Found"));
        return GetInquiryResDto.toDto(inquiry);
    }
}
