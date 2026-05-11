package com.project.ds_helper.domain.reply.dto.response;

import com.project.ds_helper.domain.reply.entity.Reply;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "문의 ID 기준 답변 조회 DTO", example = "{\"replyId\":\"reply-001\",\"content\":\"안녕하세요. 문의 주신 내용 답변드립니다.\",\"inquiryId\":\"inquiry-001\"}")
public record GetReplyByInquiryIdResDto(
        String replyId,
        String content,
        String inquiryId
) {
    public static Object toEmptyDto(){
        return GetReplyByInquiryIdResDto.builder()
                .replyId("")
                .content("")
                .inquiryId("")
                .build();
    }

    public static GetReplyByInquiryIdResDto toDto(Reply reply){
        return GetReplyByInquiryIdResDto.builder()
                .replyId(reply.getId())
                .content(reply.getContent())
                .inquiryId(reply.getInquiry().getId())
                .build();
    }
}
