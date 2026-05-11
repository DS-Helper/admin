package com.project.ds_helper.domain.admin.controller;

import com.project.ds_helper.common.dto.response.JwtResponse;
import com.project.ds_helper.domain.admin.dto.request.AdminLoginReqDto;
import com.project.ds_helper.domain.admin.service.AdminAuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuthControllerTest {

    @Mock
    private AdminAuthService adminAuthService;

    @InjectMocks
    private AdminAuthController adminAuthController;

    @Test
    @DisplayName("관리자 로그인은 서비스 결과를 반환한다")
    void adminLogin_returnsServiceResult() {
        JwtResponse jwtResponse = new JwtResponse("access", "refresh");
        when(adminAuthService.adminLogin(any(AdminLoginReqDto.class), any())).thenReturn(jwtResponse);

        ResponseEntity<JwtResponse> response = adminAuthController.adminLogin(new AdminLoginReqDto(), null);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(jwtResponse);
    }
}
