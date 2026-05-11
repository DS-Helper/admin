package com.project.ds_helper.common.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 모든 엔드포인트
                .allowedOrigins("http://localhost:3000", "http://127.0.0.1:5500", "http://localhost:8080",
                        "https://test.dshelper.kro.kr", "https://client.dshelper.kro.kr",
                        // 도메인 변경하여 새로 CORS 등록 (dshelper.kr)
                        "https://www.dshelper.kr", "https://server.dshelper.kr") // 허용할 도메인
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS") // 허용할 메서드
                .allowedHeaders("*") // 모든 헤더 허용
                .allowCredentials(true); // 인증정보 포함 허용
    }
}
