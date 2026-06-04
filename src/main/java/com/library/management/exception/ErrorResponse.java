package com.library.management.exception;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(
        ErrorCode code,
        String message,
        Map<String, String> fieldErrors,
        LocalDateTime timestamp
) {

    public ErrorResponse(
            ErrorCode code,
            String message
    ) {
        this(code, message, null, LocalDateTime.now());
    }

    public ErrorResponse(
            ErrorCode code,
            String message,
            Map<String, String> fieldErrors
    ) {
        this(code, message, fieldErrors, LocalDateTime.now());
    }
}