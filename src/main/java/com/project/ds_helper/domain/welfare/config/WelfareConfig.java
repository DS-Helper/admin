package com.project.ds_helper.domain.welfare.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 복지 서비스 관련 설정 클래스
 */
@Configuration
@EnableScheduling
public class WelfareConfig {

    @Value("${public.data.portal.service.key}")
    private String serviceKey;

    /**
     * 공공데이터포털 API 호출을 위한 WebClient 빈 등록
     */
    @Bean
    public WebClient publicDataWebClient() {
        return WebClient.builder()
                .baseUrl("https://apis.data.go.kr/B554287/LocalGovernmentWelfareInformations")
                .defaultHeader("Accept", "application/json") // JSON 응답 선호
                .build();
    }

    /**
     * 서비스키를 반환하는 빈 (보안을 위해 캡슐화)
     */
    @Bean
    public String publicDataServiceKey() {
        return serviceKey;
    }
}
