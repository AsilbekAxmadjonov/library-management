package com.library.management.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
    private final HttpStatus httpStatus;

    public BusinessException(ErrorCode errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public static BusinessException notFound(String resource, Long id) {
        return new BusinessException(
                ErrorCode.RESOURCE_NOT_FOUND,
                resource + " not found with id: " + id,
                HttpStatus.NOT_FOUND
        );
    }
}
