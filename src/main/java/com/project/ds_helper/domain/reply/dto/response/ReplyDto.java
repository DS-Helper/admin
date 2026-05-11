package com.project.ds_helper.domain.reply.dto.response;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.project.ds_helper.domain.inquiry.dto.response.InquiryDto;
import com.project.ds_helper.domain.reply.entity.Reply;
import com.project.ds_helper.domain.user.dto.response.UserDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Schema(description = "답변 DTO", example = "{\"replyId\":\"reply-001\",\"content\":\"안녕하세요. 문의 주신 내용 답변드립니다.\",\"user\":{\"userId\":\"admin-001\",\"email\":\"admin@test.com\",\"role\":\"ADMIN\",\"name\":\"관리자\",\"gender\":\"\"}}")
public class ReplyDto {

    private String replyId;

    private String content;

    private UserDto user;

    public static ReplyDto toDto(Reply reply){
        return ReplyDto.builder()
                .replyId(reply.getId())
                .content(reply.getContent())
                .user(UserDto.toDto(reply.getUser()))
                .build();
    }
}
