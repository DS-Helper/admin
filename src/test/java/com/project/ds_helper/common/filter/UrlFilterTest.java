package com.project.ds_helper.common.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class UrlFilterTest {

    @Test
    @DisplayName("공개 경로와 로그아웃 경로를 판별한다")
    void urlChecks_work() {
        assertThat(UrlFilter.isPublicPath("/auth/login/organization")).isTrue();
        assertThat(UrlFilter.checkIfPublicPath("/auth/login/organization")).isTrue();
        assertThat(UrlFilter.checkIfLogoutPath("/logout")).isTrue();
        assertThat(UrlFilter.checkIfLogoutPath("/other")).isFalse();
        assertThat(UrlFilter.checkIfPublicPathForPosts(new MockHttpServletRequest("GET", "/posts/1"))).isTrue();
        assertThat(UrlFilter.checkIfPublicPathForPosts(new MockHttpServletRequest("POST", "/posts/1"))).isFalse();
        assertThat(UrlFilter.getSecurityFilterPassPath()).isNotEmpty();
    }
}
