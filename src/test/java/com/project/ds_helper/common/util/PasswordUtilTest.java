package com.project.ds_helper.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordUtilTest {

    @Test
    @DisplayName("비밀번호가 비어 있으면 false를 반환한다")
    void isPasswordMatch_returnsFalseWhenBlank() {
        PasswordUtil passwordUtil = new PasswordUtil();

        assertThat(passwordUtil.isPasswordMatch("", "a")).isFalse();
        assertThat(passwordUtil.isPasswordMatch("a", "")).isFalse();
    }

    @Test
    @DisplayName("비밀번호가 같으면 true를 반환한다")
    void isPasswordMatch_returnsTrueWhenEqual() {
        PasswordUtil passwordUtil = new PasswordUtil();

        assertThat(passwordUtil.isPasswordMatch("pw", "pw")).isTrue();
    }

    @Test
    @DisplayName("bcrypt encoder bean을 만든다")
    void bCryptPasswordEncoder_createsBean() {
        BCryptPasswordEncoder encoder = new PasswordUtil().bCryptPasswordEncoder();

        assertThat(encoder).isNotNull();
    }
}
