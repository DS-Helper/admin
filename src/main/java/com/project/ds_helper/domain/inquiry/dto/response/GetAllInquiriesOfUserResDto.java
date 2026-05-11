package com.project.ds_helper.domain.inquiry.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import com.project.ds_helper.common.dto.response.PageResponseDto;
import com.project.ds_helper.domain.inquiry.entity.Inquiry;
import com.project.ds_helper.domain.inquiry.entity.InquiryImage;
import com.project.ds_helper.domain.reply.dto.response.ReplyDto;
import com.project.ds_helper.domain.user.dto.response.UserDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Schema(description = "사용자 문의 목록 응답 DTO", example = "{\"inquiries\":[{\"inquiryId\":\"inquiry-001\",\"content\":\"서비스 이용 방법을 알고 싶어요.\",\"type\":\"기타\",\"status\":\"답변대기\",\"user\":{\"userId\":\"user-001\",\"email\":\"user@test.com\",\"role\":\"USER\",\"name\":\"홍길동\",\"gender\":\"남성\"},\"imageUrls\":[\"https://cdn.dshelper.kr/inquiry/1.png\"],\"createdAt\":\"2026-03-23 14:00\",\"replyCreatedAt\":null}],\"page\":{\"page\":0,\"size\":10,\"totalElements\":1,\"totalPages\":1,\"first\":true,\"last\":true,\"hasNext\":false,\"hasPrevious\":false,\"sort\":{\"sorted\":true,\"unsorted\":false,\"empty\":false}}}")
public record GetAllInquiriesOfUserResDto(
        List<ThisInquiry> inquiries,
        PageResponseDto page
) {


    @Builder
    public record ThisInquiry(
            String inquiryId,
            String content,
            String type,
            String status,
            UserDto user,
            List<String> imageUrls,
            @JsonSerialize(using = LocalDateTimeSerializer.class)
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
            LocalDateTime createdAt,
            @JsonSerialize(using = LocalDateTimeSerializer.class)
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
            LocalDateTime updatedAt,
            ReplyDto reply){

        public static ThisInquiry toThisInquiry(Inquiry inquiry){
            return ThisInquiry.builder()
                    .inquiryId(inquiry.getId())
                    .content(inquiry.getContent())
                    .type(inquiry.getType().getKorean())
                    .status(inquiry.getStatus().getKorean())
                    .user(UserDto.toDto(inquiry.getUser()))
                    .imageUrls(inquiry.getImages().stream().map(InquiryImage::getUrl).toList())
                    .createdAt(inquiry.getCreatedAt())
                    .updatedAt(inquiry.getUpdatedAt())
                    .reply(inquiry.getReply() == null? null : com.project.ds_helper.domain.reply.dto.response.ReplyDto.toDto(inquiry.getReply())).build();
        }
    }

    public static GetAllInquiriesOfUserResDto toDto(Page<Inquiry> inquiries){
        PageResponseDto page = new PageResponseDto(inquiries.getNumber(), inquiries.getSize(), inquiries.getTotalElements(), inquiries.getTotalPages(), inquiries.isFirst(), inquiries.isLast(), inquiries.hasNext(), inquiries.hasPrevious(), inquiries.getSort());
        return new GetAllInquiriesOfUserResDto(inquiries.stream().map(ThisInquiry::toThisInquiry ).toList(), page);

//        return GetAllInquiriesOfUserResDto.builder()
//                .inquiryId(inquiry.getId())
//                .content(inquiry.getContent())
//                .type(inquiry.getType().getKorean())
//                .status(inquiry.getStatus().getKorean())
//                .user(UserDto.toDto(inquiry.getUser()))
//                .reply(ReplyDto.toDto(inquiry.getReply()))
//                .build();
    }

//    public static Map<String, List<GetAllInquiriesOfUserResDto>> toDtoList(List<Inquiry> inquiries){
//        return inquiries.stream().map(GetAllInquiriesOfUserResDto::toDto).collect(Collectors.groupingBy(GetAllInquiriesOfUserResDto::getType));
//    }


}
