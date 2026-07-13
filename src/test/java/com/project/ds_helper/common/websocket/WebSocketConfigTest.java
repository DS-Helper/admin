package com.project.ds_helper.common.websocket;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.StompWebSocketEndpointRegistration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

class WebSocketConfigTest {

    @Test
    @DisplayName("웹소켓 설정 메서드는 예외 없이 실행된다")
    void configMethods_execute() {
        CustomHandShakeInterceptor interceptor = mock(CustomHandShakeInterceptor.class);
        WebSocketConfig config = new WebSocketConfig(interceptor);
        StompEndpointRegistry registry = mock(StompEndpointRegistry.class);
        StompWebSocketEndpointRegistration endpointRegistration = mock(StompWebSocketEndpointRegistration.class);
        when(registry.addEndpoint("/ws")).thenReturn(endpointRegistration);
        when(endpointRegistration.setAllowedOriginPatterns("http://localhost:3000")).thenReturn(endpointRegistration);
        when(endpointRegistration.addInterceptors(interceptor)).thenReturn(endpointRegistration);

        assertThatCode(() -> config.registerStompEndpoints(registry)).doesNotThrowAnyException();
        assertThatCode(() -> config.configureMessageBroker(mock(MessageBrokerRegistry.class))).doesNotThrowAnyException();
    }
}
