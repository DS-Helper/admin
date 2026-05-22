package com.project.ds_helper.domain.user.service;

import com.project.ds_helper.common.util.JwtUtil;
import com.project.ds_helper.domain.user.dto.response.WithdrawUserResponseDto;
import com.project.ds_helper.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserWithdrawalServiceTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Test
    @DisplayName("회원 탈퇴 공통 처리는 고정 Clock 기준 삭제일시를 저장하고 refresh token을 삭제한다")
    void softDeleteAndDeleteRefreshToken_savesFixedDeletedAtAndDeletesRefreshToken() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-05-21T05:30:00Z"), ZoneId.of("Asia/Seoul"));
        UserWithdrawalService service = new UserWithdrawalService(fixedClock, jwtUtil, stringRedisTemplate);
        User user = User.builder()
                .id("user-1")
                .email("user@test.com")
                .build();
        when(jwtUtil.toRedisRefreshTokenKey("user-1")).thenReturn("refresh:user-1");

        WithdrawUserResponseDto result = service.softDeleteAndDeleteRefreshToken(user);

        LocalDateTime expectedDeletedAt = LocalDateTime.of(2026, 5, 21, 14, 30);
        assertThat(user.isDeleted()).isTrue();
        assertThat(user.getDeletedAt()).isEqualTo(expectedDeletedAt);
        assertThat(result.userId()).isEqualTo("user-1");
        assertThat(result.deletedAt()).isEqualTo(expectedDeletedAt);
        verify(stringRedisTemplate).delete("refresh:user-1");
    }
}
