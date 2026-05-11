package com.project.ds_helper.domain.admin.service;

import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.admin.repository.AdminInquiryRepository;
import com.project.ds_helper.domain.inquiry.dto.response.GetAllInquiriesOfUserResDto;
import com.project.ds_helper.domain.inquiry.enums.InquiryStatus;
import com.project.ds_helper.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminInquiryService {

    private final UserUtil userUtil;
    private final AdminInquiryRepository adminInquiryRepository;

    /**
     * 관리자가 미답변된 유저의 전체 문의를 조회하는 메소드 (임시 관리자 인증 제거)
     *
     * @Param Authentication authentication, int page, int size, String sort, String sortBy
     * @Return GetAllInquiriesOfUserResDto
     * **/
    public GetAllInquiriesOfUserResDto getAllUnRepliedInquiriesOfUser(int page, int size, String sort, String sortBy) {
        log.debug("AdminInquiryService.getAllUnRepliedInquiriesOfUser started. page={}, size={}, sort={}, sortBy={}", page, size, sort, sortBy);
        // pageRequest
        Pageable pageRequest = PageRequest.of(page, Math.min(size, 100), sort.equalsIgnoreCase("desc")? Sort.Direction.DESC : Sort.Direction.ASC, "createdAt");
        return GetAllInquiriesOfUserResDto.toDto(adminInquiryRepository.findAllByStatus(InquiryStatus.UNANSWERED, pageRequest));
    }
}
