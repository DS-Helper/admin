package com.project.ds_helper.common.enums;

import com.project.ds_helper.common.dto.response.ResponseCode;
import org.springframework.http.HttpStatus;

public enum SuccessCode implements ResponseCode {

    OK("OK", "요청이 성공했습니다.", HttpStatus.OK),
    CREATED("CREATED", "리소스가 성공적으로 생성되었습니다.", HttpStatus.CREATED),
    NO_CONTENT("NO_CONTENT", "요청이 성공했으며 반환할 내용이 없습니다.", HttpStatus.NO_CONTENT);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    SuccessCode(String code, String message, HttpStatus httpStatus) {
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
