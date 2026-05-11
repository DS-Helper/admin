package com.project.ds_helper.domain.user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ds_helper.common.dto.response.JwtResponse;
import com.project.ds_helper.common.util.CookieUtil;
import com.project.ds_helper.common.util.JwtUtil;
import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.user.dto.request.MobileKakaoLoginRequestDto;
import com.project.ds_helper.domain.user.dto.request.OauthWithdrawRequestDto;
import com.project.ds_helper.domain.user.dto.response.KakaoUserResponse;
import com.project.ds_helper.domain.user.entity.KakaoOauth;
import com.project.ds_helper.domain.user.entity.User;
import com.project.ds_helper.domain.user.enums.OauthType;
import com.project.ds_helper.domain.user.enums.UserRole;
import com.project.ds_helper.domain.user.enums.UserType;
import com.project.ds_helper.domain.user.repository.KakaoOauthRepository;
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
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

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
class KakaoOauthServiceTest {

    @Mock private WebClient kakaoOauthWebClient;
    @Mock private RestTemplate restTemplate;
    @Mock private JwtUtil jwtUtil;
    @Mock private CookieUtil cookieUtil;
    @Mock private ObjectMapper objectMapper;
    @Mock private KakaoOauthRepository kakaoOauthRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserUtil userUtil;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private HttpServletResponse response;

    @InjectMocks
    private KakaoOauthService service;

    @Test
    @DisplayName("카카오 로그인 URL을 생성한다")
    void getKakaoLoginUrl_returnsUrl() {
        org.springframework.test.util.ReflectionTestUtils.setField(service, "clientId", "cid");
        org.springframework.test.util.ReflectionTestUtils.setField(service, "redirectUri", "https://redirect");

        String result = service.getKakaoLoginUrl();

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
    @DisplayName("access token 기반 카카오 로그인은 기존 회원이면 JWT를 반환한다")
    void kakaoLogin_withAccessToken_returnsJwtForExistingUser() throws IOException {
        KakaoOauthService spyService = spy(service);
        MobileKakaoLoginRequestDto dto = new MobileKakaoLoginRequestDto(OauthType.KAKAO, "kakao-access-token");
        KakaoUserResponse userResponse = createKakaoUserResponse(1L, "user@test.com", "홍길동", "male", "2000", "https://img", "010-1234-5678");
        doReturn(userResponse).when(spyService).fetchUserInfo("kakao-access-token");

        User user = User.builder().id("user-id").role(UserRole.USER).type(UserType.PERSONAL).build();
        KakaoOauth kakaoOauth = KakaoOauth.builder()
                .user(user)
                .socialOauthId(1L)
                .oauthEmail("user@test.com")
                .build();
        when(kakaoOauthRepository.findBySocialOauthIdAndOauthEmail(1L, "user@test.com"))
                .thenReturn(Optional.of(kakaoOauth));
        when(jwtUtil.generateAccessToken("user-id", "USER", "PERSONAL")).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken("user-id", "USER", "PERSONAL")).thenReturn("refresh-token");
        when(jwtUtil.getRefreshTokenExpirationTime()).thenReturn(1000L);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        JwtResponse result = spyService.kakaoLogin(dto, response);

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        verify(userRepository, times(0)).save(any(User.class));
    }

    @Test
    @DisplayName("access token 기반 카카오 로그인은 신규 회원이면 가입 후 JWT를 반환한다")
    void kakaoLogin_withAccessToken_createsUserAndReturnsJwt() throws IOException {
        KakaoOauthService spyService = spy(service);
        MobileKakaoLoginRequestDto dto = new MobileKakaoLoginRequestDto(OauthType.KAKAO, "kakao-access-token");
        KakaoUserResponse userResponse = createKakaoUserResponse(2L, "new@test.com", "신규", "female", "2001", "https://img2", "010-9999-9999");
        doReturn(userResponse).when(spyService).fetchUserInfo("kakao-access-token");

        when(kakaoOauthRepository.findBySocialOauthIdAndOauthEmail(2L, "new@test.com"))
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

        JwtResponse result = spyService.kakaoLogin(dto, response);

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        verify(userRepository).save(any(User.class));
        verify(kakaoOauthRepository).save(any(KakaoOauth.class));
    }

    @Test
    @DisplayName("카카오 OAuth 회원 탈퇴는 unlink 후 연결을 해제한다")
    void withdraw_unlinksAndDisconnectsUser() {
        KakaoOauthService spyService = spy(service);
        org.springframework.security.core.Authentication authentication = org.mockito.Mockito.mock(org.springframework.security.core.Authentication.class);
        OauthWithdrawRequestDto dto = new OauthWithdrawRequestDto("kakao-access-token");
        User user = User.builder()
                .id("user-1")
                .email("user@test.com")
                .kakaoOauthConnected(true)
                .build();
        KakaoOauth kakaoOauth = KakaoOauth.builder().user(user).build();

        doNothing().when(spyService).unlink("kakao-access-token");
        when(userUtil.extractUserId(authentication)).thenReturn("user-1");
        when(userUtil.findUserById("user-1")).thenReturn(user);
        when(kakaoOauthRepository.findByUser_Id("user-1")).thenReturn(Optional.of(kakaoOauth));
        when(jwtUtil.toRedisRefreshTokenKey("user-1")).thenReturn("refresh:user-1");

        spyService.withdraw(authentication, dto);

        verify(stringRedisTemplate).delete("refresh:user-1");
        assertThat(user.isDeleted()).isTrue();
        assertThat(user.getDeletedAt()).isNotNull();
        assertThat(user.getEmail()).isEqualTo("user@test.com");
    }

    @Test
    @DisplayName("카카오 신규 가입 시 동일 이메일이 있으면 가입을 거절한다")
    void kakaoLogin_withAccessToken_throwsWhenEmailExists() throws IOException {
        KakaoOauthService spyService = spy(service);
        MobileKakaoLoginRequestDto dto = new MobileKakaoLoginRequestDto(OauthType.KAKAO, "kakao-access-token");
        KakaoUserResponse userResponse = createKakaoUserResponse(2L, "dup@test.com", "중복", "female", "2001", "https://img2", "010-9999-9999");
        doReturn(userResponse).when(spyService).fetchUserInfo("kakao-access-token");

        when(kakaoOauthRepository.findBySocialOauthIdAndOauthEmail(2L, "dup@test.com"))
                .thenReturn(Optional.empty());
        when(userRepository.existsByEmail("dup@test.com")).thenReturn(true);

        assertThatThrownBy(() -> spyService.kakaoLogin(dto, response))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email Already Exist");
    }

    private KakaoUserResponse createKakaoUserResponse(
            Long socialOauthId,
            String email,
            String name,
            String gender,
            String birthyear,
            String profileImageUrl,
            String phoneNumber
    ) {
        KakaoUserResponse response = new KakaoUserResponse();
        response.setSocialOauthId(socialOauthId);

        KakaoUserResponse.KakaoAccount account = new KakaoUserResponse.KakaoAccount();
        account.setEmail(email);
        account.setName(name);
        account.setGender(gender);
        account.setBirthyear(birthyear);
        account.setPhoneNumber(phoneNumber);

        KakaoUserResponse.KakaoAccount.Profile profile = new KakaoUserResponse.KakaoAccount.Profile();
        profile.setProfileImageUrl(profileImageUrl);
        account.setProfile(profile);

        response.setKakaoAccount(account);
        return response;
    }
}
