package com.project.ds_helper.domain.inquiry.dto.request;

import com.project.ds_helper.domain.inquiry.entity.Inquiry;
import com.project.ds_helper.domain.inquiry.enums.InquiryType;
import com.project.ds_helper.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CreateInquiryReqDtoTest {

    @Test
    @DisplayName("문의 요청은 엔티티로 변환된다")
    void toInquiry_mapsFields() {
        CreateInquiryReqDto dto = new CreateInquiryReqDto();
        dto.setType("기타");
        dto.setContent("도와주세요");

        Inquiry inquiry = dto.toInquiry(dto, User.builder().id("user-1").build());

        assertThat(inquiry.getType()).isEqualTo(InquiryType.OTHER);
        assertThat(inquiry.getContent()).isEqualTo("도와주세요");
    }
}
