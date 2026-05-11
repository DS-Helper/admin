package com.project.ds_helper.domain.user.controller;

import com.project.ds_helper.common.dto.response.JwtResponse;
import com.project.ds_helper.common.dto.response.ResponseVo;
import com.project.ds_helper.common.enums.SuccessCode;
import com.project.ds_helper.domain.user.dto.request.GoogleCodeLoginRequestDto;
import com.project.ds_helper.domain.user.service.GoogleOAuthService;
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
class GoogleAuthControllerTest {

    @Mock
    private GoogleOAuthService googleOAuthService;

    @InjectMocks
    private GoogleAuthController googleAuthController;

    @Test
    @DisplayName("구글 로그인 URL 조회는 서비스 결과를 반환한다")
    void getGoogleLoginUrl_returnsServiceResult() {
        when(googleOAuthService.getGoogleLoginUrl()).thenReturn("https://google");

        ResponseEntity<String> result = googleAuthController.getGoogleLoginUrl();

        assertThat(result.getBody()).isEqualTo("https://google");
    }

    @Test
    @DisplayName("구글 로그인은 authorization code 기반으로 JWT 응답을 반환한다")
    void googleOauthLogin_returnsJwtResponse() throws Exception {
        GoogleCodeLoginRequestDto dto = new GoogleCodeLoginRequestDto("google-authorization-code");
        JwtResponse jwtResponse = new JwtResponse("access-token", "refresh-token");
        when(googleOAuthService.googleOauthLogin("google-authorization-code")).thenReturn(jwtResponse);

        ResponseEntity<ResponseVo<JwtResponse>> result = googleAuthController.googleOauthLogin(dto);

        assertThat(result.getStatusCode()).isEqualTo(SuccessCode.OK.getHttpStatus());
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getData()).isEqualTo(jwtResponse);
        verify(googleOAuthService).googleOauthLogin("google-authorization-code");
    }
}
