package com.project.ds_helper.domain.user.controller;

import com.project.ds_helper.common.dto.response.JwtResponse;
import com.project.ds_helper.common.dto.response.ResponseVo;
import com.project.ds_helper.common.enums.SuccessCode;
import com.project.ds_helper.domain.user.dto.request.KakaoCodeLoginRequestDto;
import com.project.ds_helper.domain.user.service.KakaoOauthService;
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
class KakaoAuthControllerTest {

    @Mock
    private KakaoOauthService kakaoOauthService;

    @InjectMocks
    private KakaoAuthController kakaoAuthController;

    @Test
    @DisplayName("카카오 로그인 URL 조회는 서비스 결과를 반환한다")
    void getKakaoLoginUrl_returnsServiceResult() throws Exception {
        when(kakaoOauthService.getKakaoLoginUrl()).thenReturn("https://kakao");

        ResponseEntity<String> result = kakaoAuthController.getKakaoLoginUrl(null);

        assertThat(result.getBody()).isEqualTo("https://kakao");
    }

    @Test
    @DisplayName("카카오 로그인은 authorization code 기반으로 JWT 응답을 반환한다")
    void kakaoLogin_returnsJwtResponse() throws Exception {
        KakaoCodeLoginRequestDto dto = new KakaoCodeLoginRequestDto("kakao-authorization-code");
        JwtResponse jwtResponse = new JwtResponse("access-token", "refresh-token");
        when(kakaoOauthService.kakaoLogin("kakao-authorization-code")).thenReturn(jwtResponse);

        ResponseEntity<ResponseVo<JwtResponse>> result = kakaoAuthController.kakaoLogin(dto);

        assertThat(result.getStatusCode()).isEqualTo(SuccessCode.OK.getHttpStatus());
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().getData()).isEqualTo(jwtResponse);
        verify(kakaoOauthService).kakaoLogin("kakao-authorization-code");
    }
}
