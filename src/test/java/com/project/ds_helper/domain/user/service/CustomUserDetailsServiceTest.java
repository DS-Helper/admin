package com.project.ds_helper.domain.user.service;

import com.project.ds_helper.domain.user.dto.CustomUserDetails;
import com.project.ds_helper.domain.user.entity.User;
import com.project.ds_helper.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private BCryptPasswordEncoder encoder;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("이메일로 사용자를 찾으면 CustomUserDetails를 반환한다")
    void loadUserByUsername_returnsCustomUserDetails() {
        User user = User.builder().id("user-1").email("user@test.com").password("encoded").build();
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        UserDetails details = customUserDetailsService.loadUserByUsername("user@test.com");

        assertThat(details).isInstanceOf(CustomUserDetails.class);
        assertThat(((CustomUserDetails) details).getId()).isEqualTo("user-1");
    }

    @Test
    @DisplayName("이메일에 해당하는 사용자가 없으면 예외가 발생한다")
    void loadUserByUsername_throwsWhenMissing() {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("missing@test.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("USER_NOT_FOUND");
    }

    @Test
    @DisplayName("삭제된 사용자는 로그인할 수 없다")
    void loadUserByUsername_throwsWhenDeletedUser() {
        User user = User.builder().id("user-1").email("user@test.com").password("encoded").isDeleted(true).build();
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("user@test.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DELETED_USER");
    }
}
