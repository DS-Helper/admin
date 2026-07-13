package com.project.ds_helper.common.exception;

import com.project.ds_helper.common.enums.ErrorCode;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.apache.coyote.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.validation.BindException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("비즈니스 예외는 코드 메시지로 응답한다")
    void handleBusinessException() {
        var response = handler.handleBusinessException(new BusinessException(ErrorCode.CONFLICT, "conflict"));
        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.CONFLICT.getHttpStatus());
    }

    @Test
    @DisplayName("잘못된 요청 예외는 BAD_REQUEST/VALIDATION_ERROR로 변환된다")
    void handleBadRequestExceptions() {
        var response = handler.handleBadRequestExceptions(new IllegalArgumentException("bad"));
        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.BAD_REQUEST.getHttpStatus());
        assertThat(handler.handleBadRequestExceptions(new BindException(new Object(), "obj")).getStatusCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR.getHttpStatus());
        assertThat(handler.handleBadRequestExceptions(mock(ConstraintViolationException.class)).getStatusCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR.getHttpStatus());
        assertThat(handler.handleBadRequestExceptions(new MissingServletRequestParameterException("a", "String")).getStatusCode())
                .isEqualTo(ErrorCode.BAD_REQUEST.getHttpStatus());
        assertThat(handler.handleBadRequestExceptions(mock(MethodArgumentNotValidException.class)).getStatusCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR.getHttpStatus());
        assertThat(handler.handleBadRequestExceptions(mock(MethodArgumentTypeMismatchException.class)).getStatusCode())
                .isEqualTo(ErrorCode.BAD_REQUEST.getHttpStatus());
        assertThat(handler.handleBadRequestExceptions(new BadRequestException("bad request")).getStatusCode())
                .isEqualTo(ErrorCode.BAD_REQUEST.getHttpStatus());
        assertThat(handler.handleBadRequestExceptions(mock(HttpMessageNotReadableException.class)).getStatusCode())
                .isEqualTo(ErrorCode.BAD_REQUEST.getHttpStatus());
    }

    @Test
    @DisplayName("리소스 없으면 NOT_FOUND")
    void handleEntityNotFound() {
        var response = handler.handleEntityNotFoundException(new EntityNotFoundException("missing"));
        assertThat(response.getStatusCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND.getHttpStatus());
    }

    @Test
    @DisplayName("나머지 예외 분기들도 공통 응답으로 내려간다")
    void handleCommonExceptions() {
        assertThat(handler.handleFileAccessDeniedException(new java.nio.file.AccessDeniedException("x")).getStatusCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED.getHttpStatus());
        assertThat(handler.handleSpringAccessDeniedException(mock(org.springframework.security.access.AccessDeniedException.class)).getStatusCode())
                .isEqualTo(ErrorCode.ACCESS_DENIED.getHttpStatus());
        assertThat(handler.handleConflictException(new IllegalStateException("state")).getStatusCode()).isEqualTo(ErrorCode.CONFLICT.getHttpStatus());
        assertThat(handler.handleAuthenticationServiceException(new AuthenticationServiceException("auth")).getStatusCode()).isEqualTo(ErrorCode.UNAUTHORIZED.getHttpStatus());
        assertThat(handler.handleRedisConnectionFailureException(new RedisConnectionFailureException("redis")).getStatusCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus());
        assertThat(handler.handleDataAccessException(new DataAccessResourceFailureException("db")).getStatusCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus());
        assertThat(handler.handleNullPointerException(new NullPointerException()).getStatusCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus());
        assertThat(handler.handleException(new RuntimeException("x")).getStatusCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus());
    }
}
