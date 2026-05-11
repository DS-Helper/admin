package com.project.ds_helper.common.websocket;

import com.project.ds_helper.domain.user.entity.User;
import com.project.ds_helper.domain.user.enums.UserRole;
import com.project.ds_helper.domain.user.repository.UserRepository;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.messaging.simp.user.SimpSubscription;
import org.springframework.messaging.simp.user.SimpSubscriptionMatcher;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.security.Principal;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RequiredArgsConstructor
@Slf4j
@Component
public class CustomHandShakeInterceptor implements HandshakeInterceptor {

    private final UserRepository userRepository;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        
        // principal 획득
        Principal principal = request.getPrincipal();

        // principal이 null 이거나 name이 없다면 false 반환
        if(principal == null || principal.getName().isEmpty() || principal.getName().isBlank()) {
            log.info("No Principal Or No Name");
            return false;
        }

        // name 획득
        String name = principal.getName();
        log.info("[BeforeHandShake] name : {}", name);

        // Optional 유저 획득
        Optional<User> optionalUser = userRepository.findByEmail(name);
        if(optionalUser.isEmpty()){
            log.info("[BeforeHandShake] optionalUser is Empty");
            return false;
        }
        log.info("[BeforeHandShake] optionalUser get");

        // 유저 get
        User user = optionalUser.get();
        log.info("[BeforeHandShake] user : {} get", user.getEmail());

        // email, role 저장
        Object email = user.getEmail();
        Object role = user.getRole();
        attributes.putAll(Map.of(
                "email", email,
                "role", role)
        );
        log.info("[BeforeHandShake] attributes email : {}, role : {} saved", email, role );

        return true; 
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        Map<String, Object> attributes = request.getAttributes();
        Object email = attributes.get("email");
        Object role = attributes.get("role");
        log.info("[AfterHandShake] email : {}, role : {}", email, role);

    }
}
