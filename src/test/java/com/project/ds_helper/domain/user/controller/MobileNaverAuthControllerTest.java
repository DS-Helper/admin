package com.project.ds_helper.domain.user.controller;

import com.project.ds_helper.common.dto.response.JwtResponse;
import com.project.ds_helper.common.dto.response.ResponseVo;
import com.project.ds_helper.domain.user.dto.request.MobileNaverLoginRequestDto;
import com.project.ds_helper.domain.user.enums.OauthType;
import com.project.ds_helper.domain.user.service.MobileNaverOauthService;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MobileNaverAuthControllerTest {

    @Mock
    private MobileNaverOauthService mobileNaverOauthService;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private MobileNaverAuthController controller;

    @Test
    @DisplayName("모바일 네이버 로그인은 JWT 응답을 반환한다")
    void mobileNaverLogin_returnsJwtResponse() throws Exception {
        MobileNaverLoginRequestDto dto = new MobileNaverLoginRequestDto(OauthType.NAVER, "token");
        JwtResponse jwtResponse = new JwtResponse("access", "refresh");
        when(mobileNaverOauthService.mobileNaverLogin(dto, response)).thenReturn(jwtResponse);

        ResponseEntity<ResponseVo<JwtResponse>> result = controller.mobileNaverLogin(dto, response);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody().getData()).isEqualTo(jwtResponse);
    }
}
