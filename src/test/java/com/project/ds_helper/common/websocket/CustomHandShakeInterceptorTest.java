package com.project.ds_helper.common.websocket;

import com.project.ds_helper.domain.user.entity.User;
import com.project.ds_helper.domain.user.enums.UserRole;
import com.project.ds_helper.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CustomHandShakeInterceptorTest {

    @Test
    @DisplayName("핸드셰이크는 principal 없으면 실패한다")
    void beforeHandshake_returnsFalseWhenPrincipalMissing() throws Exception {
        CustomHandShakeInterceptor interceptor = new CustomHandShakeInterceptor(mock(UserRepository.class));
        ServerHttpRequest request = mock(ServerHttpRequest.class);

        boolean result = interceptor.beforeHandshake(request, mock(ServerHttpResponse.class), mock(WebSocketHandler.class), new HashMap<>());

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("핸드셰이크는 사용자 조회 성공 시 속성을 저장한다")
    void beforeHandshake_savesAttributes() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        User user = User.builder().email("user@test.com").role(UserRole.ADMIN).build();
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        CustomHandShakeInterceptor interceptor = new CustomHandShakeInterceptor(userRepository);
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        when(request.getPrincipal()).thenReturn((Principal) () -> "user@test.com");
        Map<String, Object> attributes = new HashMap<>();

        boolean result = interceptor.beforeHandshake(request, mock(ServerHttpResponse.class), mock(WebSocketHandler.class), attributes);

        assertThat(result).isTrue();
        assertThat(attributes).containsEntry("email", "user@test.com").containsEntry("role", UserRole.ADMIN);
    }
}
