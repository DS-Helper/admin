package com.project.ds_helper.domain.admin.service;

import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.admin.repository.AdminInquiryRepository;
import com.project.ds_helper.domain.inquiry.dto.response.GetAllInquiriesOfUserResDto;
import com.project.ds_helper.domain.inquiry.entity.Inquiry;
import com.project.ds_helper.domain.inquiry.enums.InquiryStatus;
import com.project.ds_helper.domain.inquiry.enums.InquiryType;
import com.project.ds_helper.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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

    public GetAllInquiriesOfUserResDto getInquiries(
            String inquiryId,
            InquiryStatus status,
            InquiryType type,
            LocalDate startDate,
            LocalDate endDate,
            int page,
            int size,
            String sort,
            String sortBy
    ) {
        log.debug("AdminInquiryService.getInquiries started. inquiryId={}, status={}, type={}, dateRange={}~{}",
                inquiryId, status, type, startDate, endDate);

        Pageable pageRequest = PageRequest.of(
                page,
                Math.min(size, 100),
                sort.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC,
                sortBy
        );

        Specification<Inquiry> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (inquiryId != null && !inquiryId.isBlank()) predicates.add(cb.equal(root.get("id"), inquiryId));
            if (status != null) predicates.add(cb.equal(root.get("status"), status));
            if (type != null) predicates.add(cb.equal(root.get("type"), type));
            if (startDate != null) predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), startDate.atStartOfDay()));
            if (endDate != null) predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), endDate.atTime(23, 59, 59)));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return GetAllInquiriesOfUserResDto.toDto(adminInquiryRepository.findAll(spec, pageRequest));
    }
}
