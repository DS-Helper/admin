package com.project.ds_helper.domain.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "문의 답변 수정 요청 DTO")
public class UpdateInquiryReplyReqDto {

    @NotBlank(message = "답변 내용을 입력해주십시오.")
    @Schema(description = "답변 내용", example = "문의 주신 내용 답변드립니다.")
    private String content;
}
