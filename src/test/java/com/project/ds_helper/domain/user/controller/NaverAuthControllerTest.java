package com.project.ds_helper.domain.user.controller;

import com.project.ds_helper.common.dto.response.JwtResponse;
import com.project.ds_helper.common.dto.response.ResponseVo;
import com.project.ds_helper.common.enums.SuccessCode;
import com.project.ds_helper.domain.user.dto.request.NaverCodeLoginRequestDto;
import com.project.ds_helper.domain.user.service.NaverOauthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NaverAuthControllerTest {

    @Mock
    private NaverOauthService naverOauthService;

    @InjectMocks
    private NaverAuthController naverAuthController;

    @Test
    @DisplayName("네이버 로그인 URL 조회는 서비스 결과를 반환한다")
    void naverLoginUrl_returnsServiceResult() {
        when(naverOauthService.naverLoginUrl()).thenReturn("https://naver");

        ResponseEntity<String> result = naverAuthController.naverLoginUrl();

        assertThat(result.getBody()).isEqualTo("https://naver");
    }

    @Test
    @DisplayName("네이버 로그인은 authorization code와 state 기반으로 JWT 응답을 반환한다")
    void naverLogin_returnsJwtResponse() throws Exception {
        NaverCodeLoginRequestDto dto = new NaverCodeLoginRequestDto("naver-authorization-code", "naver-state");
        JwtResponse jwtResponse = new JwtResponse("access-token", "refresh-token");
        when(naverOauthService.naverLogin("naver-authorization-code", "naver-state")).thenReturn(jwtResponse);

        ResponseEntity<ResponseVo<JwtResponse>> result = naverAuthController.naverLogin(dto);

        assertThat(result.getStatusCode()).isEqualTo(SuccessCode.OK.getHttpStatus());
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getData()).isEqualTo(jwtResponse);
        verify(naverOauthService).naverLogin("naver-authorization-code", "naver-state");
    }
}
