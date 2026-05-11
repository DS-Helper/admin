package com.project.ds_helper.domain.user.controller;

import com.project.ds_helper.common.dto.response.JwtResponse;
import com.project.ds_helper.common.dto.response.ResponseVo;
import com.project.ds_helper.domain.user.dto.request.MobileGoogleLoginRequestDto;
import com.project.ds_helper.domain.user.enums.OauthType;
import com.project.ds_helper.domain.user.service.MobileGoogleOauthService;
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
class MobileGoogleAuthControllerTest {

    @Mock
    private MobileGoogleOauthService mobileGoogleOauthService;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private MobileGoogleAuthController controller;

    @Test
    @DisplayName("모바일 구글 로그인은 JWT 응답을 반환한다")
    void mobileGoogleLogin_returnsJwtResponse() throws Exception {
        MobileGoogleLoginRequestDto dto = new MobileGoogleLoginRequestDto(OauthType.GOOGLE, "token");
        JwtResponse jwtResponse = new JwtResponse("access", "refresh");
        when(mobileGoogleOauthService.mobileGoogleLogin(dto, response)).thenReturn(jwtResponse);

        ResponseEntity<ResponseVo<JwtResponse>> result = controller.mobileGoogleLogin(dto, response);

        assertThat(result.getStatusCode().value()).isEqualTo(200);
        assertThat(result.getBody().getData()).isEqualTo(jwtResponse);
    }
}
