package com.project.ds_helper.common.dto.response;

import org.springframework.http.HttpStatus;

public interface ResponseCode {
    String getCode();
    String getMessage();
    HttpStatus getHttpStatus();
}
