package com.project.ds_helper.domain.user.service;

import com.project.ds_helper.common.dto.response.JwtResponse;
import com.project.ds_helper.common.util.CookieUtil;
import com.project.ds_helper.common.util.JwtUtil;
import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.user.dto.request.MobileGoogleLoginRequestDto;
import com.project.ds_helper.domain.user.dto.request.OauthWithdrawRequestDto;
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

import java.io.IOException;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doNothing;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GoogleOAuthServiceTest {

    @Mock private GoogleOauthRepository googleOauthRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserUtil userUtil;
    @Mock private JwtUtil jwtUtil;
    @Mock private CookieUtil cookieUtil;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private HttpServletResponse response;

    @InjectMocks
    private GoogleOAuthService service;

    @Test
    @DisplayName("구글 로그인 URL을 생성한다")
    void getGoogleLoginUrl_returnsUrl() {
        ReflectionTestUtils.setField(service, "clientId", "cid");
        ReflectionTestUtils.setField(service, "redirectUri", "https://redirect");

        String result = service.getGoogleLoginUrl();

        assertThat(result).contains("client_id=cid").contains("redirect_uri=https://redirect");
    }

    @Test
    @DisplayName("JWT 생성 메서드는 refresh token을 redis에 저장하고 cookie 헤더를 추가한다")
    void generateJwtTokenAndPutInCookie_savesRedisAndAddsCookies() {
        when(jwtUtil.generateAccessToken("u1", "USER", "PERSONAL")).thenReturn("access");
        when(jwtUtil.generateRefreshToken("u1", "USER", "PERSONAL")).thenReturn("refresh");
        when(jwtUtil.toRedisRefreshTokenKey("u1")).thenReturn("refresh:u1");
        when(jwtUtil.getRefreshTokenExpirationTime()).thenReturn(1000L);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(cookieUtil.addAccessTokenCookie("access")).thenReturn(org.springframework.http.ResponseCookie.from("a", "a").build());
        when(cookieUtil.addRefreshTokenCookie("refresh")).thenReturn(org.springframework.http.ResponseCookie.from("r", "r").build());

        service.generateJwtTokenAndPutInCookie(response, "u1", "USER", "PERSONAL");

        verify(valueOperations).set("refresh:u1", "refresh", Duration.ofMillis(1000L));
        verify(response, times(2)).addHeader(
                org.mockito.ArgumentMatchers.eq(org.springframework.http.HttpHeaders.SET_COOKIE),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    @DisplayName("access token 기반 구글 로그인은 기존 회원이면 JWT를 반환한다")
    void googleOauthLogin_withAccessToken_returnsJwtForExistingUser() throws IOException {
        GoogleOAuthService spyService = spy(service);
        MobileGoogleLoginRequestDto dto = new MobileGoogleLoginRequestDto(OauthType.GOOGLE, "google-access-token");
        GoogleUserInfoResponse userInfo = new GoogleUserInfoResponse();
        org.springframework.test.util.ReflectionTestUtils.setField(userInfo, "id", "social-id");
        org.springframework.test.util.ReflectionTestUtils.setField(userInfo, "email", "user@test.com");
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

        JwtResponse result = spyService.googleOauthLogin(dto, response);

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        verify(userRepository, times(0)).save(any(User.class));
    }

    @Test
    @DisplayName("access token 기반 구글 로그인은 신규 회원이면 가입 후 JWT를 반환한다")
    void googleOauthLogin_withAccessToken_createsUserAndReturnsJwt() throws IOException {
        GoogleOAuthService spyService = spy(service);
        MobileGoogleLoginRequestDto dto = new MobileGoogleLoginRequestDto(OauthType.GOOGLE, "google-access-token");
        GoogleUserInfoResponse userInfo = new GoogleUserInfoResponse();
        org.springframework.test.util.ReflectionTestUtils.setField(userInfo, "id", "social-id");
        org.springframework.test.util.ReflectionTestUtils.setField(userInfo, "email", "new@test.com");
        doReturn(userInfo).when(spyService).getUserInfo("google-access-token");

        when(googleOauthRepository.findBySocialOauthIdAndOauthEmail("social-id", "new@test.com"))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User saved = invocation.getArgument(0);
                    saved.setId("new-user-id");
                    return saved;
                });
        when(jwtUtil.generateAccessToken("new-user-id", "USER", "PERSONAL")).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken("new-user-id", "USER", "PERSONAL")).thenReturn("refresh-token");
        when(jwtUtil.getRefreshTokenExpirationTime()).thenReturn(1000L);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        JwtResponse result = spyService.googleOauthLogin(dto, response);

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        verify(userRepository).save(any(User.class));
        verify(googleOauthRepository).save(any(GoogleOauth.class));
    }

    @Test
    @DisplayName("구글 OAuth 회원 탈퇴는 토큰을 revoke하고 연결을 해제한다")
    void withdraw_revokesTokenAndDisconnectsUser() {
        GoogleOAuthService spyService = spy(service);
        org.springframework.security.core.Authentication authentication = org.mockito.Mockito.mock(org.springframework.security.core.Authentication.class);
        OauthWithdrawRequestDto dto = new OauthWithdrawRequestDto("google-access-token");
        User user = User.builder()
                .id("user-1")
                .email("user@test.com")
                .googleOauthConnected(true)
                .build();
        GoogleOauth googleOauth = GoogleOauth.builder().user(user).build();

        doNothing().when(spyService).revokeAccessToken("google-access-token");
        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(userUtil.findUserById("user-1")).thenReturn(user);
        when(googleOauthRepository.findByUser_Id("user-1")).thenReturn(Optional.of(googleOauth));
        when(jwtUtil.toRedisRefreshTokenKey("user-1")).thenReturn("refresh:user-1");

        spyService.withdraw(authentication, dto);

        verify(stringRedisTemplate).delete("refresh:user-1");
        assertThat(user.isDeleted()).isTrue();
        assertThat(user.getDeletedAt()).isNotNull();
        assertThat(user.getEmail()).isEqualTo("user@test.com");
    }

    @Test
    @DisplayName("구글 신규 가입 시 동일 이메일이 있으면 가입을 거절한다")
    void googleOauthLogin_withAccessToken_throwsWhenEmailExists() throws IOException {
        GoogleOAuthService spyService = spy(service);
        MobileGoogleLoginRequestDto dto = new MobileGoogleLoginRequestDto(OauthType.GOOGLE, "google-access-token");
        GoogleUserInfoResponse userInfo = new GoogleUserInfoResponse();
        ReflectionTestUtils.setField(userInfo, "id", "social-id");
        ReflectionTestUtils.setField(userInfo, "email", "dup@test.com");
        doReturn(userInfo).when(spyService).getUserInfo("google-access-token");

        when(googleOauthRepository.findBySocialOauthIdAndOauthEmail("social-id", "dup@test.com"))
                .thenReturn(Optional.empty());
        when(userRepository.existsByEmail("dup@test.com")).thenReturn(true);

        assertThatThrownBy(() -> spyService.googleOauthLogin(dto, response))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email Already Exist");
    }
}
