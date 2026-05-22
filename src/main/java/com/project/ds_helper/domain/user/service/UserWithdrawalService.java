package com.project.ds_helper.domain.user.service;

import com.project.ds_helper.common.util.JwtUtil;
import com.project.ds_helper.domain.user.dto.response.WithdrawUserResponseDto;
import com.project.ds_helper.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class UserWithdrawalService {

    private final Clock clock;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;

    public UserWithdrawalService(
            Clock clock,
            JwtUtil jwtUtil,
            @Qualifier("CustomStringRedisTemplate") StringRedisTemplate stringRedisTemplate
    ) {
        this.clock = clock;
        this.jwtUtil = jwtUtil;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public WithdrawUserResponseDto softDeleteAndDeleteRefreshToken(User user) {
        LocalDateTime deletedAt = LocalDateTime.now(clock);
        user.softDelete(deletedAt);
        stringRedisTemplate.delete(jwtUtil.toRedisRefreshTokenKey(user.getId()));
        return new WithdrawUserResponseDto(user.getId(), deletedAt);
    }
}
