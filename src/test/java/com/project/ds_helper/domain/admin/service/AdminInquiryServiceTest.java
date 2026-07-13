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
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

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

    @Test
    @DisplayName("臾몄쓽 ?꾩껜 議고쉶???꾪꽣??DTO濡?蹂?섑븳??")
    void getInquiries_returnsDto() {
        Inquiry inquiry = Inquiry.builder()
                .id("inquiry-1")
                .content("臾몄쓽")
                .type(InquiryType.OTHER)
                .status(InquiryStatus.UNANSWERED)
                .user(User.builder().id("user-1").name("tester").build())
                .build();
        Page<Inquiry> page = new PageImpl<>(List.of(inquiry));
        when(adminInquiryRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(Pageable.class)))
                .thenReturn(page);

        GetAllInquiriesOfUserResDto result = adminInquiryService.getInquiries("inquiry-1", InquiryStatus.UNANSWERED, InquiryType.OTHER, null, null, 0, 10, "desc", "createdAt");

        assertThat(result.inquiries()).hasSize(1);
        assertThat(result.inquiries().getFirst().inquiryId()).isEqualTo("inquiry-1");
    }

    @Test
    @DisplayName("문의 전체 조회 Specification은 inquiryId/status/type/date 조건을 포함해 Predicate를 생성한다")
    void getInquiries_specification_buildsPredicates() {
        Page<Inquiry> page = new PageImpl<>(List.of());
        when(adminInquiryRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        adminInquiryService.getInquiries(
                "inquiry-1",
                InquiryStatus.UNANSWERED,
                InquiryType.OTHER,
                java.time.LocalDate.of(2026, 5, 1),
                java.time.LocalDate.of(2026, 5, 2),
                0,
                10,
                "desc",
                "createdAt"
        );

        ArgumentCaptor<Specification<Inquiry>> captor = ArgumentCaptor.forClass(Specification.class);
        verify(adminInquiryRepository).findAll(captor.capture(), any(Pageable.class));

        jakarta.persistence.criteria.Root<Inquiry> root =
                org.mockito.Mockito.mock(jakarta.persistence.criteria.Root.class, org.mockito.Answers.RETURNS_MOCKS);
        jakarta.persistence.criteria.CriteriaQuery<?> query =
                org.mockito.Mockito.mock(jakarta.persistence.criteria.CriteriaQuery.class, org.mockito.Answers.RETURNS_MOCKS);
        jakarta.persistence.criteria.CriteriaBuilder cb =
                org.mockito.Mockito.mock(jakarta.persistence.criteria.CriteriaBuilder.class, org.mockito.Answers.RETURNS_MOCKS);

        jakarta.persistence.criteria.Predicate built = captor.getValue().toPredicate(root, query, cb);
        assertThat(built).isNotNull();
    }

    @Test
    @DisplayName("미답변 문의 조회는 UNANSWERED 상태만 사용한다")
    void getAllUnRepliedInquiriesOfUser_usesUnansweredStatus() {
        when(adminInquiryRepository.findAllByStatus(eq(InquiryStatus.UNANSWERED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        adminInquiryService.getAllUnRepliedInquiriesOfUser(0, 10, "asc", "createdAt");

        verify(adminInquiryRepository).findAllByStatus(eq(InquiryStatus.UNANSWERED), any(Pageable.class));
    }

    @Test
    @DisplayName("문의 전체 조회는 조건이 없어도 동작한다")
    void getInquiries_withoutFilters() {
        when(adminInquiryRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        GetAllInquiriesOfUserResDto result = adminInquiryService.getInquiries(null, null, null, null, null, 0, 10, "asc", "createdAt");

        assertThat(result.inquiries()).isEmpty();
    }
}
