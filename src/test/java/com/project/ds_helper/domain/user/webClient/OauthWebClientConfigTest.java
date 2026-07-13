package com.project.ds_helper.domain.user.webClient;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OauthWebClientConfigTest {

    @Test
    @DisplayName("OAuth WebClient 설정들은 WebClient를 생성한다")
    void buildsClients() {
        WebClient.Builder builder = mock(WebClient.Builder.class);
        when(builder.baseUrl(anyString())).thenReturn(builder);
        when(builder.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)).thenReturn(builder);
        when(builder.build()).thenReturn(WebClient.builder().build());

        KakaoOauthWebClientConfig kakao = new KakaoOauthWebClientConfig();
        GoogleOauthWebClientConfig google = new GoogleOauthWebClientConfig();
        NaverOauthWebClientConfig naver = new NaverOauthWebClientConfig();

        assertThat(kakao.kakaoOauthWebClient(builder)).isNotNull();
        assertThat(kakao.kakaoApiWebClient(builder)).isNotNull();
        assertThat(google.googleOauthWebClient(builder)).isNotNull();
        assertThat(naver.naverOauthWebClient(builder)).isNotNull();
    }
}
