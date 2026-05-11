package com.project.ds_helper.domain.inquiry.dto.request;

import com.project.ds_helper.domain.inquiry.entity.Inquiry;
import com.project.ds_helper.domain.inquiry.entity.InquiryImage;
import com.project.ds_helper.domain.inquiry.enums.InquiryType;
import com.project.ds_helper.domain.post.entity.PostImage;
import com.project.ds_helper.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class CreateInquiryReqDto {

    @NotNull
    @Schema(example = "기타")
    private String type;

    @NotNull
    @Schema(example = "기타 줄좀 고쳐줘요")
    private String content;

    public Inquiry toInquiry(CreateInquiryReqDto dto, User user){
        return Inquiry.builder()
                .content(dto.getContent())
                .type(InquiryType.findByKorean(dto.getType()))
                .user(user)
                .build();
    }

}
