package com.hrms.common.exceptions;

import org.springframework.http.HttpStatus;

public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(ErrorCode.CONFLICT, HttpStatus.CONFLICT, message);
    }
}
