package com.project.ds_helper.domain.user.service;

import com.project.ds_helper.common.dto.response.JwtResponse;
import com.project.ds_helper.common.util.CookieUtil;
import com.project.ds_helper.common.util.JwtUtil;
import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.user.dto.request.MobileNaverLoginRequestDto;
import com.project.ds_helper.domain.user.dto.request.OauthWithdrawRequestDto;
import com.project.ds_helper.domain.user.entity.NaverOauth;
import com.project.ds_helper.domain.user.entity.User;
import com.project.ds_helper.domain.user.enums.OauthType;
import com.project.ds_helper.domain.user.enums.UserRole;
import com.project.ds_helper.domain.user.enums.UserType;
import com.project.ds_helper.domain.user.repository.NaverOauthRepository;
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

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
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
class NaverOauthServiceTest {

    @Mock private JwtUtil jwtUtil;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private CookieUtil cookieUtil;
    @Mock private RestTemplate restTemplate;
    @Mock private UserUtil userUtil;
    @Mock private UserRepository userRepository;
    @Mock private NaverOauthRepository naverOauthRepository;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private HttpServletResponse response;

    @InjectMocks
    private NaverOauthService service;

    @Test
    @DisplayName("네이버 로그인 URL을 생성한다")
    void naverLoginUrl_returnsUrl() {
        ReflectionTestUtils.setField(service, "clientId", "cid");
        ReflectionTestUtils.setField(service, "redirectUri", "https://redirect");

        String result = service.naverLoginUrl();

        assertThat(result).contains("client_id=cid").contains("redirect_uri=");
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
    @DisplayName("access token 기반 네이버 로그인은 기존 회원이면 JWT를 반환한다")
    void naverLogin_withAccessToken_returnsJwtForExistingUser() {
        NaverOauthService spyService = spy(service);
        MobileNaverLoginRequestDto dto = new MobileNaverLoginRequestDto(OauthType.NAVER, "naver-access-token");
        doReturn(createNaverUserInfo("social-id", "user@test.com", "홍길동", "M"))
                .when(spyService).fetchUserInfoFromNaver("naver-access-token");

        User user = User.builder().id("user-id").role(UserRole.USER).type(UserType.PERSONAL).build();
        NaverOauth naverOauth = NaverOauth.builder()
                .user(user)
                .socialOauthId("social-id")
                .oauthEmail("user@test.com")
                .build();
        when(naverOauthRepository.findBySocialOauthIdAndOauthEmail("social-id", "user@test.com"))
                .thenReturn(Optional.of(naverOauth));
        when(jwtUtil.generateAccessToken("user-id", "USER", "PERSONAL")).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken("user-id", "USER", "PERSONAL")).thenReturn("refresh-token");
        when(jwtUtil.getRefreshTokenExpirationTime()).thenReturn(1000L);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        JwtResponse result = spyService.naverLogin(dto, response);

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        verify(userRepository, times(0)).save(any(User.class));
    }

    @Test
    @DisplayName("access token 기반 네이버 로그인은 gender가 없어도 신규 회원 가입 후 JWT를 반환한다")
    void naverLogin_withAccessToken_allowsNullGender() {
        NaverOauthService spyService = spy(service);
        MobileNaverLoginRequestDto dto = new MobileNaverLoginRequestDto(OauthType.NAVER, "naver-access-token");
        doReturn(createNaverUserInfo("social-id", "new@test.com", "신규", null))
                .when(spyService).fetchUserInfoFromNaver("naver-access-token");

        when(naverOauthRepository.findBySocialOauthIdAndOauthEmail("social-id", "new@test.com"))
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

        JwtResponse result = spyService.naverLogin(dto, response);

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        verify(userRepository).save(any(User.class));
        verify(naverOauthRepository).save(any(NaverOauth.class));
    }

    @Test
    @DisplayName("네이버 OAuth 회원 탈퇴는 토큰 폐기 후 연결을 해제한다")
    void withdraw_revokesTokenAndDisconnectsUser() {
        NaverOauthService spyService = spy(service);
        org.springframework.security.core.Authentication authentication = org.mockito.Mockito.mock(org.springframework.security.core.Authentication.class);
        OauthWithdrawRequestDto dto = new OauthWithdrawRequestDto("naver-access-token");
        User user = User.builder()
                .id("user-1")
                .email("user@test.com")
                .naverOauthConnected(true)
                .build();
        NaverOauth naverOauth = NaverOauth.builder().user(user).build();

        doNothing().when(spyService).revokeToken("naver-access-token");
        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(userUtil.findUserById("user-1")).thenReturn(user);
        when(naverOauthRepository.findByUser_Id("user-1")).thenReturn(Optional.of(naverOauth));
        when(jwtUtil.toRedisRefreshTokenKey("user-1")).thenReturn("refresh:user-1");

        spyService.withdraw(authentication, dto);

        verify(stringRedisTemplate).delete("refresh:user-1");
        assertThat(user.isDeleted()).isTrue();
        assertThat(user.getDeletedAt()).isNotNull();
        assertThat(user.getEmail()).isEqualTo("user@test.com");
    }

    @Test
    @DisplayName("네이버 신규 가입 시 동일 이메일이 있으면 가입을 거절한다")
    void naverLogin_withAccessToken_throwsWhenEmailExists() {
        NaverOauthService spyService = spy(service);
        MobileNaverLoginRequestDto dto = new MobileNaverLoginRequestDto(OauthType.NAVER, "naver-access-token");
        doReturn(createNaverUserInfo("social-id", "dup@test.com", "중복", "M"))
                .when(spyService).fetchUserInfoFromNaver("naver-access-token");

        when(naverOauthRepository.findBySocialOauthIdAndOauthEmail("social-id", "dup@test.com"))
                .thenReturn(Optional.empty());
        when(userRepository.existsByEmail("dup@test.com")).thenReturn(true);

        assertThatThrownBy(() -> spyService.naverLogin(dto, response))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email Already Exist");
    }

    private Map<String, Object> createNaverUserInfo(String socialOauthId, String email, String name, String gender) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", socialOauthId);
        userInfo.put("profile_image", "https://img");
        userInfo.put("gender", gender);
        userInfo.put("mobile", "010-1234-5678");
        userInfo.put("birthyear", "2000");
        userInfo.put("email", email);
        userInfo.put("name", name);
        userInfo.put("token", "naver-token");
        return userInfo;
    }
}
