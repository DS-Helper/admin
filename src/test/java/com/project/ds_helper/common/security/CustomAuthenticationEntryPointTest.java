package com.project.ds_helper.common.security;

import com.project.ds_helper.common.enums.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CustomAuthenticationEntryPointTest {

    @Test
    @DisplayName("인증 실패는 unauthorized 응답을 쓴다")
    void commence_writesUnauthorized() throws Exception {
        SecurityResponseWriter writer = mock(SecurityResponseWriter.class);
        CustomAuthenticationEntryPoint entryPoint = new CustomAuthenticationEntryPoint(writer);
        HttpServletResponse response = mock(HttpServletResponse.class);

        entryPoint.commence(null, response, null);

        verify(writer).writeError(response, ErrorCode.UNAUTHORIZED);
    }
}
