package com.project.ds_helper.common.enums;

import com.project.ds_helper.common.dto.response.ResponseCode;
import org.springframework.http.HttpStatus;

public enum ErrorCode implements ResponseCode {
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    BAD_REQUEST("BAD_REQUEST", "잘못된 요청입니다.", HttpStatus.BAD_REQUEST),
    INVALID_PARAMETER("INVALID_PARAMETER", "요청 파라미터가 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    VALIDATION_ERROR("VALIDATION_ERROR", "요청값 검증에 실패했습니다.", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", "요청한 리소스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    USER_NOT_FOUND("USER_NOT_FOUND", "요청한 사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    UNAUTHORIZED("UNAUTHORIZED", "인증이 필요합니다.", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED("ACCESS_DENIED", "해당 리소스에 접근할 권한이 없습니다.", HttpStatus.FORBIDDEN),
    CONFLICT("CONFLICT", "현재 리소스 상태로는 요청을 처리할 수 없습니다.", HttpStatus.CONFLICT),
    VOLUNTEER_APPLICATION_INVALID_STATE("VOLUNTEER_APPLICATION_INVALID_STATE", "현재 상태에서는 가입 신청을 처리할 수 없습니다.", HttpStatus.CONFLICT),
    VOLUNTEER_EVENT_INVALID_STATE("VOLUNTEER_EVENT_INVALID_STATE", "현재 상태에서는 봉사 일정을 변경할 수 없습니다.", HttpStatus.CONFLICT),
    VOLUNTEER_CAPACITY_BELOW_CURRENT_PARTICIPANTS("VOLUNTEER_CAPACITY_BELOW_CURRENT_PARTICIPANTS", "현재 참여 인원보다 정원을 줄일 수 없습니다.", HttpStatus.CONFLICT),
    VOLUNTEER_ATTENDANCE_NOT_AVAILABLE("VOLUNTEER_ATTENDANCE_NOT_AVAILABLE", "종료된 일정만 출석 처리할 수 있습니다.", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
