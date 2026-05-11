package com.project.ds_helper.domain.admin.service;

import com.project.ds_helper.common.dto.response.JwtResponse;
import com.project.ds_helper.common.util.JwtUtil;
import com.project.ds_helper.domain.admin.dto.request.AdminLoginReqDto;
import com.project.ds_helper.domain.admin.entity.Admin;
import com.project.ds_helper.domain.admin.repository.AdminRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdminAuthService {

    private final AdminRepository adminRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;

    @Transactional(readOnly = true)
    public JwtResponse adminLogin(AdminLoginReqDto dto, HttpServletResponse response) {
        log.debug("AdminAuthService.adminLogin started. username={}", dto.getUsername());

        Admin admin = adminRepository.findByUsername(dto.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 관리자 계정입니다."));

        if (!bCryptPasswordEncoder.matches(dto.getPassword(), admin.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        String adminId = admin.getId();
        String role = "ADMIN";
        String type = "ADMIN";

        String accessToken = jwtUtil.generateAccessToken(adminId, role, type);
        String refreshToken = jwtUtil.generateRefreshToken(adminId, role, type);

        saveRefreshTokenWithTtl(adminId, refreshToken);

        log.debug("Admin Login Success. adminId={}", adminId);
        return new JwtResponse(accessToken, refreshToken);
    }

    private void saveRefreshTokenWithTtl(String userId, String refreshToken) {
        stringRedisTemplate.opsForValue().set(
                jwtUtil.toRedisRefreshTokenKey(userId),
                refreshToken,
                Duration.ofMillis(jwtUtil.getRefreshTokenExpirationTime())
        );
    }
}
