package com.project.ds_helper.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Discord Webhook을 이용한 알림 전송 유틸리티
 */
@Slf4j
@Component
public class DiscordWebhookUtil {

    @Value("${discord.webhook.url:}")
    private String webhookUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 지정된 Webhook URL로 메시지를 전송합니다.
     * 
     * @param content 메시지 내용
     */
    public void sendMessage(String content) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            log.warn("Discord Webhook URL이 설정되지 않아 알림을 보낼 수 없습니다.");
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = new HashMap<>();
            body.put("content", content);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(webhookUrl, entity, String.class);
            log.debug("Discord 알림 발송 완료: {}", content);
        } catch (Exception e) {
            log.error("Discord 알림 발송 중 오류 발생: {}", e.getMessage());
        }
    }
}
