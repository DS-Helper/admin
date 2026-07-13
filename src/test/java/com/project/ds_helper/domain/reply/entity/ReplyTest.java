package com.project.ds_helper.domain.reply.entity;

import com.project.ds_helper.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class ReplyTest {

    @Test
    @DisplayName("reply는 생성 시 식별자를 갖는다")
    void prePersistGeneratesId() {
        Reply reply = Reply.builder()
                .content("content")
                .user(User.builder().id("user-1").build())
                .build();

        ReflectionTestUtils.invokeMethod(reply, "perPersistGenerateId");

        assertThat(reply.getId()).isNotBlank();
    }
}
