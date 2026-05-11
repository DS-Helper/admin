package com.project.ds_helper.domain.reply.dto.request;

import com.project.ds_helper.domain.inquiry.entity.Inquiry;
import com.project.ds_helper.domain.reply.entity.Reply;
import com.project.ds_helper.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
@Getter
public class CreateReplyReqDto {

    @NotBlank
    @Schema(example = "de9c9d39-bfad-48ef-991b-1b849688744a")
    private String inquiryId;

    @NotBlank
    @Schema(example = "고객님 이용에 불편을 드려 대단히 죄송합니다. 이후 이용에 불편함이 없도록 최선을 다하겠습니다. 감사합니다. Thank You. Dui Bu Qi.")
    private String content;


    public Reply toReply(CreateReplyReqDto dto, User user){
        return Reply.builder()
                .user(user)
                .content(dto.content)
                .build();
    }


}
