package com.project.ds_helper.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
@Slf4j
public class GmailUtil {

    private final JavaMailSender javaMailSender;
    private final StringRedisTemplate stringRedisTemplate;


    public GmailUtil(JavaMailSender javaMailSender, StringRedisTemplate stringRedisTemplate) {
        this.javaMailSender = javaMailSender;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     *
     * **/
    public void sendSignUpVerificationCode(String to, String subject, String body) {

        String code = UUID.randomUUID().toString().substring(6);

        stringRedisTemplate.opsForValue().set("emailVerificationCode:" + to, code, Duration.ofMinutes(3));
        log.info("Email Verification Code Sent Successfully. To : {}, Code : {}", to, code);
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(body);
        javaMailSender.send(msg);
    }
}
