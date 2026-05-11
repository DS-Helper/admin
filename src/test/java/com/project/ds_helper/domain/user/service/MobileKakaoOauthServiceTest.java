package com.project.ds_helper.domain.user.service;

import com.project.ds_helper.common.dto.response.JwtResponse;
import com.project.ds_helper.common.util.CookieUtil;
import com.project.ds_helper.common.util.JwtUtil;
import com.project.ds_helper.domain.user.dto.request.MobileKakaoLoginRequestDto;
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
class MobileKakaoOauthServiceTest {

    @Mock
    private WebClient kakaoOauthWebClient;
    @Mock
    private WebClient kakaoApiWebClient;
    @Mock
    private KakaoOauthRepository kakaoOauthRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private CookieUtil cookieUtil;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private MobileKakaoOauthService service;

    @Test
    @DisplayName("모바일 카카오 로그인은 기존 회원에게 access token과 refresh token을 각각 발급한다")
    void mobileKakaoLogin_existingUser_returnsDistinctTokens() throws IOException {
        MobileKakaoLoginRequestDto dto = new MobileKakaoLoginRequestDto(OauthType.KAKAO, "kakao-access-token");
        MobileKakaoOauthService spyService = spy(service);
        doReturn(createKakaoUserResponse(1L, "user@test.com", "홍길동", "male", "2000", "https://img", "010-1234-5678"))
                .when(spyService).fetchUserInfo("kakao-access-token");

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

        JwtResponse result = spyService.mobileKakaoLogin(dto, response);

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(result.refreshToken()).isNotEqualTo(result.accessToken());
        verify(jwtUtil).generateRefreshToken("user-id", "USER", "PERSONAL");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("모바일 카카오 신규 가입 시 동일 이메일이 있으면 가입을 거절한다")
    void mobileKakaoLogin_throwsWhenEmailExists() throws IOException {
        MobileKakaoLoginRequestDto dto = new MobileKakaoLoginRequestDto(OauthType.KAKAO, "kakao-access-token");
        MobileKakaoOauthService spyService = spy(service);
        doReturn(createKakaoUserResponse(2L, "dup@test.com", "중복", "female", "2001", "https://img2", "010-9999-9999"))
                .when(spyService).fetchUserInfo("kakao-access-token");

        when(kakaoOauthRepository.findBySocialOauthIdAndOauthEmail(2L, "dup@test.com"))
                .thenReturn(Optional.empty());
        when(userRepository.existsByEmail("dup@test.com")).thenReturn(true);

        assertThatThrownBy(() -> spyService.mobileKakaoLogin(dto, response))
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
