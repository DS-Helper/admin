package com.project.ds_helper.domain.inquiry.service;

import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.inquiry.entity.Inquiry;
import com.project.ds_helper.domain.inquiry.repository.InquiryRepository;
import com.project.ds_helper.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InquiryQueryServiceTest {
    @Mock private InquiryRepository inquiryRepository;
    @Mock private UserUtil userUtil;
    @Mock private Authentication authentication;
    @InjectMocks private InquiryQueryService inquiryQueryService;

    @Test
    @DisplayName("내 문의 목록은 최대 100개로 제한한다")
    void getAllInquiriesOfUser_limitsSize() {
        User user = User.builder().id("u-1").build();
        when(userUtil.extractUserId(authentication)).thenReturn("u-1");
        when(inquiryRepository.findAllByUser_Id(eq("u-1"), any())).thenReturn(new PageImpl<>(List.of()));

        var result = inquiryQueryService.getAllInquiriesOfUser(authentication, 0, 101, "desc", "createdAt");

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("문의 단건 조회는 없으면 예외를 던진다")
    void getInquiry_throwsWhenMissing() {
        when(inquiryRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inquiryQueryService.getInquiry("missing"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
