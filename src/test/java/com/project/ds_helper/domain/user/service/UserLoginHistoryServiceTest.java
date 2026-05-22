package com.project.ds_helper.domain.user.service;

import com.project.ds_helper.domain.user.entity.User;
import com.project.ds_helper.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class UserLoginHistoryServiceTest {

    @Test
    @DisplayName("로그인 성공 시 고정된 시각으로 마지막 로그인 일시를 저장한다")
    void recordSuccessfulLogin_savesLastLoginAt() {
        Clock clock = Clock.fixed(Instant.parse("2026-05-22T00:10:00Z"), ZoneId.of("Asia/Seoul"));
        UserRepository userRepository = mock(UserRepository.class);
        UserLoginHistoryService service = new UserLoginHistoryService(clock, userRepository);
        User user = User.builder().id("user-1").build();

        service.recordSuccessfulLogin(user);

        assertThat(user.getLastLoginAt()).isEqualTo(LocalDateTime.of(2026, 5, 22, 9, 10));
        verify(userRepository).save(user);
    }
}
