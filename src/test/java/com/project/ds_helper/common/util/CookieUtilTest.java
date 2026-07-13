package com.project.ds_helper.common.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CookieUtilTest {

    @Test
    @DisplayName("쿠키 생성과 추출이 동작한다")
    void cookieOperations_work() {
        CookieUtil cookieUtil = new CookieUtil();

        ResponseCookie access = cookieUtil.addAccessTokenCookie("a");
        ResponseCookie refresh = cookieUtil.addRefreshTokenCookie("r");
        ResponseCookie localAccess = cookieUtil.addAccessTokenCookieForLocalhost("la");
        ResponseCookie localRefresh = cookieUtil.addRefreshTokenCookieForLocalhost("lr");
        ResponseCookie expired = cookieUtil.expireCookieByCookieName("name");

        assertThat(access.getName()).isEqualTo("accessToken");
        assertThat(refresh.getName()).isEqualTo("refreshToken");
        assertThat(localAccess.getDomain()).isEqualTo("localhost");
        assertThat(localRefresh.getDomain()).isEqualTo("localhost");
        assertThat(expired.getName()).isEqualTo("name");

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getCookies()).thenReturn(new Cookie[]{
                new Cookie("accessToken", "access"),
                new Cookie("refreshToken", "refresh")
        });
        assertThat(cookieUtil.getAuthToken(request)).isEqualTo("access");
        assertThat(cookieUtil.getRefreshTokenFromCookie(request)).isEqualTo("refresh");
    }
}
