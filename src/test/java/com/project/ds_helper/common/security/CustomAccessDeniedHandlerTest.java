package com.project.ds_helper.common.security;

import com.project.ds_helper.common.enums.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CustomAccessDeniedHandlerTest {

    @Test
    @DisplayName("인가 거부는 access denied 응답을 쓴다")
    void handle_writesAccessDenied() throws Exception {
        SecurityResponseWriter writer = mock(SecurityResponseWriter.class);
        CustomAccessDeniedHandler handler = new CustomAccessDeniedHandler(writer);
        HttpServletResponse response = mock(HttpServletResponse.class);

        handler.handle(null, response, null);

        verify(writer).writeError(response, ErrorCode.ACCESS_DENIED);
    }
}
