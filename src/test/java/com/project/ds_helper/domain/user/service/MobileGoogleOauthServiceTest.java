package com.project.ds_helper.domain.user.service;

import com.project.ds_helper.common.dto.response.JwtResponse;
import com.project.ds_helper.common.util.CookieUtil;
import com.project.ds_helper.common.util.JwtUtil;
import com.project.ds_helper.domain.user.dto.request.MobileGoogleLoginRequestDto;
import com.project.ds_helper.domain.user.dto.response.GoogleUserInfoResponse;
import com.project.ds_helper.domain.user.entity.GoogleOauth;
import com.project.ds_helper.domain.user.entity.User;
import com.project.ds_helper.domain.user.enums.OauthType;
import com.project.ds_helper.domain.user.enums.UserRole;
import com.project.ds_helper.domain.user.enums.UserType;
import com.project.ds_helper.domain.user.repository.GoogleOauthRepository;
import com.project.ds_helper.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MobileGoogleOauthServiceTest {

    @Mock
    private GoogleOauthRepository googleOauthRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private CookieUtil cookieUtil;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private MobileGoogleOauthService service;

    @Test
    @DisplayName("모바일 구글 로그인은 기존 회원에게 access token과 refresh token을 각각 발급한다")
    void mobileGoogleLogin_existingUser_returnsDistinctTokens() throws IOException {
        MobileGoogleOauthService spyService = spy(service);
        MobileGoogleLoginRequestDto dto = new MobileGoogleLoginRequestDto(OauthType.GOOGLE, "google-access-token");
        GoogleUserInfoResponse userInfo = new GoogleUserInfoResponse();
        ReflectionTestUtils.setField(userInfo, "id", "social-id");
        ReflectionTestUtils.setField(userInfo, "email", "user@test.com");
        doReturn(userInfo).when(spyService).getUserInfo("google-access-token");

        User user = User.builder().id("user-id").role(UserRole.USER).type(UserType.PERSONAL).build();
        GoogleOauth googleOauth = GoogleOauth.builder()
                .user(user)
                .socialOauthId("social-id")
                .oauthEmail("user@test.com")
                .build();

        when(googleOauthRepository.findBySocialOauthIdAndOauthEmail("social-id", "user@test.com"))
                .thenReturn(Optional.of(googleOauth));
        when(jwtUtil.generateAccessToken("user-id", "USER", "PERSONAL")).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken("user-id", "USER", "PERSONAL")).thenReturn("refresh-token");
        when(jwtUtil.getRefreshTokenExpirationTime()).thenReturn(1000L);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        JwtResponse result = spyService.mobileGoogleLogin(dto, response);

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(result.refreshToken()).isNotEqualTo(result.accessToken());
        verify(jwtUtil).generateRefreshToken("user-id", "USER", "PERSONAL");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("모바일 구글 신규 가입 시 동일 이메일이 있으면 가입을 거절한다")
    void mobileGoogleLogin_throwsWhenEmailExists() throws IOException {
        MobileGoogleOauthService spyService = spy(service);
        MobileGoogleLoginRequestDto dto = new MobileGoogleLoginRequestDto(OauthType.GOOGLE, "google-access-token");
        GoogleUserInfoResponse userInfo = new GoogleUserInfoResponse();
        ReflectionTestUtils.setField(userInfo, "id", "social-id");
        ReflectionTestUtils.setField(userInfo, "email", "dup@test.com");
        doReturn(userInfo).when(spyService).getUserInfo("google-access-token");

        when(googleOauthRepository.findBySocialOauthIdAndOauthEmail("social-id", "dup@test.com"))
                .thenReturn(Optional.empty());
        when(userRepository.existsByEmail("dup@test.com")).thenReturn(true);

        assertThatThrownBy(() -> spyService.mobileGoogleLogin(dto, response))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email Already Exist");
    }
}
