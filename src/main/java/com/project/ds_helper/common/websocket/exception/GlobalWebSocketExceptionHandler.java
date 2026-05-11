package com.project.ds_helper.common.websocket.exception;

import io.netty.handler.codec.http.websocketx.WebSocketCloseStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice
@Slf4j
public class GlobalWebSocketExceptionHandler {

    @MessageExceptionHandler(MessagingException.class)
    public ResponseEntity<?> messagingExceptionHandler(MessagingException messagingException){
        String exceptionMessage = messagingException.getMessage();
        return new ResponseEntity<>(exceptionMessage, HttpStatus.BAD_REQUEST);
    }


}
