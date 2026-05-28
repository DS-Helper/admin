package com.project.ds_helper.domain.admin.service;

import com.project.ds_helper.common.dto.response.JwtResponse;
import com.project.ds_helper.common.util.JwtUtil;
import com.project.ds_helper.domain.admin.dto.request.AdminLoginReqDto;
import com.project.ds_helper.domain.admin.entity.Admin;
import com.project.ds_helper.domain.admin.repository.AdminRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminAuthServiceTest {

    @Mock private AdminRepository adminRepository;
    @Mock private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private StringRedisTemplate stringRedisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private HttpServletResponse response;

    @InjectMocks
    private AdminAuthService adminAuthService;

    @Test
    @DisplayName("관리자 로그인 성공 시 JWT를 발급하고 refreshToken을 Redis에 저장한다")
    void adminLogin_success_savesRefreshToken() {
        AdminLoginReqDto dto = new AdminLoginReqDto();
        ReflectionTestUtils.setField(dto, "username", "admin");
        ReflectionTestUtils.setField(dto, "password", "pw");

        Admin admin = Admin.builder().id("admin-1").username("admin").password("encoded").build();
        when(adminRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(bCryptPasswordEncoder.matches("pw", "encoded")).thenReturn(true);
        when(jwtUtil.generateAccessToken("admin-1", "ADMIN", "ADMIN")).thenReturn("access");
        when(jwtUtil.generateRefreshToken("admin-1", "ADMIN", "ADMIN")).thenReturn("refresh");
        when(jwtUtil.toRedisRefreshTokenKey("admin-1")).thenReturn("refresh:admin-1");
        when(jwtUtil.getRefreshTokenExpirationTime()).thenReturn(1000L);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        JwtResponse result = adminAuthService.adminLogin(dto, response);

        assertThat(result.accessToken()).isEqualTo("access");
        assertThat(result.refreshToken()).isEqualTo("refresh");
        verify(valueOperations).set("refresh:admin-1", "refresh", Duration.ofMillis(1000L));
    }

    @Test
    @DisplayName("관리자 계정이 없으면 예외를 던진다")
    void adminLogin_throwsWhenAdminMissing() {
        AdminLoginReqDto dto = new AdminLoginReqDto();
        ReflectionTestUtils.setField(dto, "username", "missing");
        ReflectionTestUtils.setField(dto, "password", "pw");
        when(adminRepository.findByUsername("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminAuthService.adminLogin(dto, response))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("비밀번호가 불일치하면 예외를 던진다")
    void adminLogin_throwsWhenPasswordMismatch() {
        AdminLoginReqDto dto = new AdminLoginReqDto();
        ReflectionTestUtils.setField(dto, "username", "admin");
        ReflectionTestUtils.setField(dto, "password", "pw");

        Admin admin = Admin.builder().id("admin-1").username("admin").password("encoded").build();
        when(adminRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        when(bCryptPasswordEncoder.matches("pw", "encoded")).thenReturn(false);

        assertThatThrownBy(() -> adminAuthService.adminLogin(dto, response))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
