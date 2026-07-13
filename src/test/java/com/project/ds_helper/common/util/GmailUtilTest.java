package com.project.ds_helper.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GmailUtilTest {

    @Test
    @DisplayName("인증 코드 발송은 Redis와 메일 발송을 수행한다")
    void sendSignUpVerificationCode_sendsMail() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        var valueOps = mock(org.springframework.data.redis.core.ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        GmailUtil gmailUtil = new GmailUtil(mailSender, redisTemplate);

        assertThatCode(() -> gmailUtil.sendSignUpVerificationCode("a@a.com", "subject", "body"))
                .doesNotThrowAnyException();

        verify(mailSender).send(any(org.springframework.mail.SimpleMailMessage.class));
        verify(valueOps).set(org.mockito.ArgumentMatchers.startsWith("emailVerificationCode:"), any(), any());
    }
}
