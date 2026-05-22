package com.project.ds_helper.common.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ds_helper.common.dto.response.JwtResponse;
import com.project.ds_helper.common.dto.response.ResponseVo;
import com.project.ds_helper.common.enums.JwtTokenType;
import com.project.ds_helper.common.util.CookieUtil;
import com.project.ds_helper.common.util.JwtUtil;
import com.project.ds_helper.domain.user.dto.CustomUserDetails;
import com.project.ds_helper.domain.user.entity.User;
import com.project.ds_helper.domain.user.enums.UserRole;
import com.project.ds_helper.domain.user.enums.UserType;
import com.project.ds_helper.domain.user.repository.UserRepository;
import com.project.ds_helper.domain.user.service.UserLoginHistoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomLoginFilterTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private CookieUtil cookieUtil;

    @Mock
    private BCryptPasswordEncoder encoder;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserLoginHistoryService userLoginHistoryService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("기관 로그인 성공 시 ResponseBody에 JwtResponse를 내려준다")
    void successfulAuthentication_writesJwtResponseBody() throws Exception {
        CustomLoginFilter filter = new CustomLoginFilter(
                authenticationManager,
                jwtUtil,
                cookieUtil,
                encoder,
                redisTemplate,
                userRepository,
                userLoginHistoryService,
                objectMapper
        );

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        User user = User.builder()
                .id("user-1")
                .email("org@test.com")
                .role(UserRole.USER)
                .type(UserType.ORGANIZATION)
                .build();
        CustomUserDetails principal = new CustomUserDetails(user);
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                principal.getAuthorities()
        );

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(jwtUtil.generateAccessToken("user-1", "USER", "ORGANIZATION")).thenReturn("access-token");
        when(jwtUtil.generateRefreshToken("user-1", "USER", "ORGANIZATION")).thenReturn("refresh-token");
        when(jwtUtil.toRedisRefreshTokenKey("user-1")).thenReturn("refresh:user-1");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        filter.successfulAuthentication(request, response, null, authentication);

        verify(userLoginHistoryService).recordSuccessfulLogin(user);
        verify(valueOperations).set("refresh:user-1", "refresh-token");
        assertThat(response.getHeader("Authorization")).isEqualTo("access-token");
        assertThat(response.getHeader(JwtTokenType.REFRESH_TOKEN_NAME.getTokenName())).isEqualTo("refresh-token");

        ResponseVo<?> responseVo = objectMapper.readValue(response.getContentAsByteArray(), ResponseVo.class);
        assertThat(responseVo.isSuccess()).isTrue();

        JwtResponse jwtResponse = objectMapper.convertValue(responseVo.getData(), JwtResponse.class);
        assertThat(jwtResponse.accessToken()).isEqualTo("access-token");
        assertThat(jwtResponse.refreshToken()).isEqualTo("refresh-token");
    }
}
