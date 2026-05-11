package com.project.ds_helper.domain.admin.service;

import com.project.ds_helper.domain.admin.repository.AdminInquiryRepository;
import com.project.ds_helper.domain.inquiry.dto.response.GetAllInquiriesOfUserResDto;
import com.project.ds_helper.domain.inquiry.entity.Inquiry;
import com.project.ds_helper.domain.inquiry.enums.InquiryStatus;
import com.project.ds_helper.domain.inquiry.enums.InquiryType;
import com.project.ds_helper.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminInquiryServiceTest {

    @Mock
    private com.project.ds_helper.common.util.UserUtil userUtil;

    @Mock
    private AdminInquiryRepository adminInquiryRepository;

    @InjectMocks
    private AdminInquiryService adminInquiryService;

    @Test
    @DisplayName("미답변 문의 목록을 DTO로 변환한다")
    void getAllUnRepliedInquiriesOfUser_returnsDto() {
        Inquiry inquiry = Inquiry.builder()
                .id("inquiry-1")
                .content("문의")
                .type(InquiryType.OTHER)
                .status(InquiryStatus.UNANSWERED)
                .user(User.builder().id("user-1").name("tester").build())
                .build();
        Page<Inquiry> page = new PageImpl<>(List.of(inquiry));
        when(adminInquiryRepository.findAllByStatus(org.mockito.ArgumentMatchers.eq(InquiryStatus.UNANSWERED), any(Pageable.class)))
                .thenReturn(page);

        GetAllInquiriesOfUserResDto result = adminInquiryService.getAllUnRepliedInquiriesOfUser(0, 10, "desc", "createdAt");

        assertThat(result.inquiries()).hasSize(1);
        assertThat(result.inquiries().getFirst().inquiryId()).isEqualTo("inquiry-1");
    }
}
