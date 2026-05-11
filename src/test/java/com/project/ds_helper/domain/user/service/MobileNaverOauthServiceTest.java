package com.project.ds_helper.domain.user.service;

import com.project.ds_helper.common.dto.response.JwtResponse;
import com.project.ds_helper.common.util.CookieUtil;
import com.project.ds_helper.common.util.JwtUtil;
import com.project.ds_helper.common.util.UserUtil;
import com.project.ds_helper.domain.user.dto.request.MobileNaverLoginRequestDto;
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
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
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
class MobileNaverOauthServiceTest {

    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private CookieUtil cookieUtil;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private UserUtil userUtil;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NaverOauthRepository naverOauthRepository;
    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private MobileNaverOauthService service;

    @Test
    @DisplayName("모바일 네이버 로그인은 gender가 없어도 신규 회원 가입과 토큰 발급이 가능하다")
    void mobileNaverLogin_allowsNullGenderAndReturnsDistinctTokens() {
        MobileNaverOauthService spyService = spy(service);
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

        JwtResponse result = spyService.mobileNaverLogin(dto, response);

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(result.refreshToken()).isNotEqualTo(result.accessToken());
        verify(jwtUtil).generateRefreshToken("new-user-id", "USER", "PERSONAL");
        verify(userRepository).save(any(User.class));
        verify(naverOauthRepository).save(any(NaverOauth.class));
    }

    @Test
    @DisplayName("모바일 네이버 로그인은 기존 회원에게 신규 저장 없이 토큰만 발급한다")
    void mobileNaverLogin_existingUser_returnsDistinctTokens() {
        MobileNaverOauthService spyService = spy(service);
        MobileNaverLoginRequestDto dto = new MobileNaverLoginRequestDto(OauthType.NAVER, "naver-access-token");
        doReturn(createNaverUserInfo("social-id", "user@test.com", "기존", "M"))
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

        JwtResponse result = spyService.mobileNaverLogin(dto, response);

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(result.refreshToken()).isNotEqualTo(result.accessToken());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("모바일 네이버 신규 가입 시 동일 이메일이 있으면 가입을 거절한다")
    void mobileNaverLogin_throwsWhenEmailExists() {
        MobileNaverOauthService spyService = spy(service);
        MobileNaverLoginRequestDto dto = new MobileNaverLoginRequestDto(OauthType.NAVER, "naver-access-token");
        doReturn(createNaverUserInfo("social-id", "dup@test.com", "중복", "M"))
                .when(spyService).fetchUserInfoFromNaver("naver-access-token");

        when(naverOauthRepository.findBySocialOauthIdAndOauthEmail("social-id", "dup@test.com"))
                .thenReturn(Optional.empty());
        when(userRepository.existsByEmail("dup@test.com")).thenReturn(true);

        assertThatThrownBy(() -> spyService.mobileNaverLogin(dto, response))
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
