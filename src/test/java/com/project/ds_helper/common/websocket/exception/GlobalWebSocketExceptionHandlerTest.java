package com.project.ds_helper.common.websocket.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.MessagingException;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalWebSocketExceptionHandlerTest {

    @Test
    @DisplayName("웹소켓 예외는 BAD_REQUEST로 변환된다")
    void messagingExceptionHandler_returnsBadRequest() {
        GlobalWebSocketExceptionHandler handler = new GlobalWebSocketExceptionHandler();

        ResponseEntity<?> response = handler.messagingExceptionHandler(new MessagingException("boom"));

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isEqualTo("boom");
    }
}
