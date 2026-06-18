package com.library.management.exception;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(
        ErrorCode code,
        String message,
        Map<String, String> fieldErrors,
        LocalDateTime timestamp
) {
    public static ErrorResponse of(ErrorCode code, String message) {
        return new ErrorResponse(code, message, null, LocalDateTime.now());
    }

    public static ErrorResponse ofValidation(Map<String, String> fieldErrors) {
        return new ErrorResponse(
                ErrorCode.VALIDATION_ERROR,
                "Validation failed",
                fieldErrors,
                LocalDateTime.now()
        );
    }
}