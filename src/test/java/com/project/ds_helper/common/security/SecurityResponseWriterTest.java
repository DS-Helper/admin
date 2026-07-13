package com.project.ds_helper.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ds_helper.common.enums.ErrorCode;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;

class SecurityResponseWriterTest {

    @Test
    @DisplayName("보안 오류 응답을 JSON으로 기록한다")
    void writeError_writesJson() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        SecurityResponseWriter writer = new SecurityResponseWriter(objectMapper);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        StringWriter buffer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(buffer);
        Mockito.when(response.getWriter()).thenReturn(printWriter);

        writer.writeError(response, ErrorCode.UNAUTHORIZED);
        printWriter.flush();

        verify(response).setStatus(ErrorCode.UNAUTHORIZED.getHttpStatus().value());
        assertThat(buffer.toString()).contains("UNAUTHORIZED");
    }

    @Test
    @DisplayName("보안 오류 응답은 다양한 ErrorCode를 처리한다")
    void writeError_handlesDifferentCodes() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        SecurityResponseWriter writer = new SecurityResponseWriter(objectMapper);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);
        Mockito.when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        assertThatCode(() -> writer.writeError(response, ErrorCode.ACCESS_DENIED)).doesNotThrowAnyException();
        assertThatCode(() -> writer.writeError(response, ErrorCode.BAD_REQUEST)).doesNotThrowAnyException();
    }
}
