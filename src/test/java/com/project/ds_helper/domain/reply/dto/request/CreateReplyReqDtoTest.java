package com.project.ds_helper.domain.reply.dto.request;

import com.project.ds_helper.domain.reply.entity.Reply;
import com.project.ds_helper.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CreateReplyReqDtoTest {

    @Test
    @DisplayName("답변 요청은 엔티티로 변환된다")
    void toReply_mapsFields() {
        CreateReplyReqDto dto = new CreateReplyReqDto();
        dto.setInquiryId("inquiry-1");
        dto.setContent("답변");

        Reply reply = dto.toReply(dto, User.builder().id("admin-1").build());

        assertThat(reply.getContent()).isEqualTo("답변");
        assertThat(reply.getUser().getId()).isEqualTo("admin-1");
    }
}
