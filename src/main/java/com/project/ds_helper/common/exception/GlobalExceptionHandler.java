package com.project.ds_helper.common.exception;

import com.project.ds_helper.common.dto.response.ResponseVo;
import com.project.ds_helper.common.enums.ErrorCode;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolationException;
import java.nio.file.AccessDeniedException;

import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.validation.BindException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ResponseVo> handleBusinessException(BusinessException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        return buildErrorResponse(errorCode, ex, ex.getMessage());
    }

    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            BindException.class,
            ConstraintViolationException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class,
            IllegalArgumentException.class,
            BadRequestException.class
    })
    public ResponseEntity<ResponseVo> handleBadRequestExceptions(Exception ex) {
        return buildErrorResponse(resolveBadRequestCode(ex), ex);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ResponseVo> handleFileAccessDeniedException(AccessDeniedException ex) {
        return buildErrorResponse(ErrorCode.ACCESS_DENIED, ex);
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ResponseVo> handleSpringAccessDeniedException(
            org.springframework.security.access.AccessDeniedException ex
    ) {
        return buildErrorResponse(ErrorCode.ACCESS_DENIED, ex);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ResponseVo> handleEntityNotFoundException(EntityNotFoundException ex) {
        return buildErrorResponse(ErrorCode.RESOURCE_NOT_FOUND, ex);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ResponseVo> handleConflictException(IllegalStateException ex) {
        return buildErrorResponse(ErrorCode.CONFLICT, ex);
    }

    @ExceptionHandler(AuthenticationServiceException.class)
    public ResponseEntity<ResponseVo> handleAuthenticationServiceException(AuthenticationServiceException ex) {
        return buildErrorResponse(ErrorCode.UNAUTHORIZED, ex);
    }

    @ExceptionHandler(RedisConnectionFailureException.class)
    public ResponseEntity<ResponseVo> handleRedisConnectionFailureException(RedisConnectionFailureException ex) {
        return buildErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, ex);
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ResponseVo> handleDataAccessException(DataAccessException ex) {
        return buildErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, ex);
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ResponseVo> handleNullPointerException(NullPointerException ex) {
        return buildErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, ex);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseVo> handleException(Exception ex) {
        return buildErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, ex);
    }

    private ResponseEntity<ResponseVo> buildErrorResponse(ErrorCode errorCode, Exception ex) {
        return buildErrorResponse(errorCode, ex, errorCode.getMessage());
    }

    private ResponseEntity<ResponseVo> buildErrorResponse(ErrorCode errorCode, Exception ex, String message) {
        log.error("Handled exception. code={}, type={}, message={}", errorCode.getCode(), ex.getClass().getSimpleName(), ex.getMessage(), ex);
        ResponseVo responseVo = ResponseVo.error(errorCode, message);
        return new ResponseEntity<>(responseVo, errorCode.getHttpStatus());
    }

    private ErrorCode resolveBadRequestCode(Exception ex) {
        if (ex instanceof MethodArgumentNotValidException
                || ex instanceof BindException
                || ex instanceof ConstraintViolationException) {
            return ErrorCode.VALIDATION_ERROR;
        }

        if (ex instanceof IllegalArgumentException) {
            return ErrorCode.INVALID_PARAMETER;
        }

        return ErrorCode.BAD_REQUEST;
    }
}
