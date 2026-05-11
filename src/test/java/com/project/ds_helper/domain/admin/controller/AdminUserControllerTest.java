package com.project.ds_helper.domain.admin.controller;

import com.project.ds_helper.domain.admin.service.AdminUserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock
    private AdminUserService adminUserService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AdminUserController adminUserController;

    @Test
    @DisplayName("userCount는 서비스 결과를 반환한다")
    void userCount_returnsServiceResult() {
        when(adminUserService.userCount(authentication)).thenReturn(Map.of("userCount", 10));

        ResponseEntity<?> response = adminUserController.userCount(authentication);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(Map.of("userCount", 10));
    }

    @Test
    @DisplayName("이름 기반 사용자 정보 조회는 서비스 결과를 반환한다")
    void getUserInfoByName_returnsServiceResult() {
        when(adminUserService.getUserInfoByName("tester", authentication)).thenReturn(List.of("result"));

        ResponseEntity<?> response = adminUserController.getUserInfoByName("tester", authentication);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(List.of("result"));
    }
}
