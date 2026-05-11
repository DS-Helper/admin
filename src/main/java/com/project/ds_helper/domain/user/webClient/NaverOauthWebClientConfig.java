package com.project.ds_helper.domain.user.webClient;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class NaverOauthWebClientConfig {

    @Bean(name = "naverOauthWebClient")
    public WebClient naverOauthWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl("https://kauth.kakao.com")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
