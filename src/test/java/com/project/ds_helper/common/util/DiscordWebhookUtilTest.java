package com.project.ds_helper.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;

class DiscordWebhookUtilTest {

    @Test
    @DisplayName("웹훅 URL이 없으면 예외 없이 종료한다")
    void sendMessage_returnsWhenWebhookMissing() {
        DiscordWebhookUtil util = new DiscordWebhookUtil();
        ReflectionTestUtils.setField(util, "webhookUrl", "");

        assertThatCode(() -> util.sendMessage("hello")).doesNotThrowAnyException();
    }
}
