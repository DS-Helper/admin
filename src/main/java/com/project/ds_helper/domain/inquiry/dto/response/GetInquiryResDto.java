package com.project.ds_helper.domain.inquiry.dto.response;

import com.project.ds_helper.domain.inquiry.entity.Inquiry;
import com.project.ds_helper.domain.reply.dto.response.ReplyDto;
import com.project.ds_helper.domain.user.dto.response.UserDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@Schema(description = "문의 단건 조회 응답 DTO", example = "{\"inquiryId\":\"inquiry-001\",\"content\":\"서비스 이용 방법을 알고 싶어요.\",\"type\":\"기타\",\"status\":\"답변완료\",\"user\":{\"userId\":\"user-001\",\"email\":\"user@test.com\",\"role\":\"USER\",\"name\":\"홍길동\",\"gender\":\"남성\"},\"reply\":{\"replyId\":\"reply-001\",\"content\":\"안녕하세요. 이용 가이드를 안내드립니다.\",\"user\":{\"userId\":\"admin-001\",\"email\":\"admin@test.com\",\"role\":\"ADMIN\",\"name\":\"관리자\",\"gender\":\"\"}}}")
public class GetInquiryResDto {

    private String inquiryId;

    private String content;

    private String type;

    private String status;

    private UserDto user;

    private ReplyDto reply;

    public static GetInquiryResDto toDto(Inquiry inquiry){
        return GetInquiryResDto.builder()
                .inquiryId(inquiry.getId())
                .content(inquiry.getContent())
                .type(inquiry.getType().getKorean())
                .status(inquiry.getStatus().getKorean())
                .user(UserDto.toDto(inquiry.getUser()))
                .reply(inquiry.getReply() == null? null : ReplyDto.toDto(inquiry.getReply()))
                .build();
    }
}
