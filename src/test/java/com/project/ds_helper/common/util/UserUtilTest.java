package com.project.ds_helper.common.util;

import com.project.ds_helper.common.exception.user.UserNotFoundException;
import com.project.ds_helper.domain.user.entity.User;
import com.project.ds_helper.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserUtilTest {

    @Test
    @DisplayName("인증 객체에서 사용자 ID를 추출한다")
    void extractUserId_returnsPrincipal() {
        UserUtil userUtil = new UserUtil(mock(UserRepository.class));
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn("user-1");

        assertThat(userUtil.extractUserId(authentication)).isEqualTo("user-1");
    }

    @Test
    @DisplayName("userId로 사용자를 조회한다")
    void findUserById_returnsUser() {
        UserRepository repository = mock(UserRepository.class);
        when(repository.findById("user-1")).thenReturn(Optional.of(User.builder().id("user-1").build()));
        UserUtil userUtil = new UserUtil(repository);

        assertThat(userUtil.findUserById("user-1").getId()).isEqualTo("user-1");
    }

    @Test
    @DisplayName("삭제된 사용자는 예외가 발생한다")
    void findUserById_throwsWhenDeleted() {
        UserRepository repository = mock(UserRepository.class);
        User deletedUser = User.builder().id("user-1").build();
        deletedUser.softDelete(java.time.LocalDateTime.now());
        when(repository.findById("user-1")).thenReturn(Optional.of(deletedUser));
        UserUtil userUtil = new UserUtil(repository);

        assertThatThrownBy(() -> userUtil.findUserById("user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Deleted User");
    }

    @Test
    @DisplayName("존재 여부를 repository에 위임한다")
    void existsByEmail_delegates() {
        UserRepository repository = mock(UserRepository.class);
        when(repository.existsByEmail("a@a.com")).thenReturn(true);
        UserUtil userUtil = new UserUtil(repository);

        assertThat(userUtil.existsByEmail("a@a.com")).isTrue();
    }
}
