package com.project.ds_helper.domain.user.controller;

import com.project.ds_helper.common.util.CookieUtil;
import com.project.ds_helper.common.util.JwtUtil;
import com.project.ds_helper.domain.user.dto.request.OrganizationJoinReqDto;
import com.project.ds_helper.domain.user.dto.request.UserJoinReqDto;
import com.project.ds_helper.domain.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private CookieUtil cookieUtil;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private AuthController authController;

    @Test
    @DisplayName("로그인 여부 확인은 refreshToken 헤더를 읽어 서비스에 위임한다")
    void checkIfUserLoggedIn_returnsServiceResult() {
        when(jwtUtil.getRefreshTokenFromRequestHeader(request)).thenReturn("refresh-token");
        when(userService.checkIfUserLoggedIn("refresh-token")).thenReturn(true);

        ResponseEntity<?> result = authController.checkIfUserLoggedIn(request);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody()).isEqualTo(true);
    }

    @Test
    @DisplayName("일반 회원 가입은 서비스 호출 후 200을 반환한다")
    void userJoin_returnsOk() throws Exception {
        UserJoinReqDto dto = UserJoinReqDto.builder()
                .email("user@test.com")
                .password("test1234")
                .passwordCheck("test1234")
                .build();

        ResponseEntity<?> result = authController.userJoin(dto, response);

        verify(userService).userJoin(dto, response);
        assertThat(result.getStatusCode().value()).isEqualTo(200);
    }

    @Test
    @DisplayName("기관 가입은 서비스 호출 후 200을 반환한다")
    void organizationJoin_returnsOk() throws Exception {
        OrganizationJoinReqDto dto = OrganizationJoinReqDto.builder()
                .email("org@test.com")
                .password("test1234")
                .passwordCheck("test1234")
                .organizationName("기관")
                .organizationPhoneNumber("010-1234-5678")
                .build();

        ResponseEntity<?> result = authController.organizationJoin(dto, List.of());

        verify(userService).organizationJoin(dto, List.of());
        assertThat(result.getStatusCode().value()).isEqualTo(200);
    }
}
