package com.library.management.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(
            BusinessException ex, HttpServletRequest request) {
        log.warn("Business exception [{}]: {} | path={}",
                ex.getErrorCode(), ex.getMessage(), request.getRequestURI());
        return ResponseEntity
                .status(ex.getHttpStatus())
                .body(ErrorResponse.of(ex.getErrorCode(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null
                                ? fe.getDefaultMessage() : "Invalid value",
                        (existing, replacement) -> existing
                ));
        log.warn("Validation failed: {} | path={}",
                fieldErrors, request.getRequestURI());
        return ResponseEntity
                .badRequest()
                .body(ErrorResponse.ofValidation(fieldErrors));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(
            HttpServletRequest request) {
        log.warn("Optimistic lock conflict | path={}", request.getRequestURI());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(
                        ErrorCode.CONCURRENT_MODIFICATION,
                        "This resource was modified by another request. Please try again."
                ));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Data integrity violation: {} | path={}",
                ex.getMostSpecificCause().getMessage(), request.getRequestURI());

        String message = ex.getMostSpecificCause().getMessage();
        if (message != null && message.contains("isbn")) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ErrorResponse.of(
                            ErrorCode.DUPLICATE_RESOURCE,
                            "A book with this ISBN already exists"
                    ));
        }

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(
                        ErrorCode.CONCURRENT_MODIFICATION,
                        "Operation failed due to a data conflict. Please try again."
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(
            Exception ex, HttpServletRequest request) {
        log.error("Unexpected error | path={}", request.getRequestURI(), ex);
        return ResponseEntity
                .internalServerError()
                .body(ErrorResponse.of(
                        ErrorCode.INTERNAL_ERROR,
                        "An unexpected error occurred"
                ));
    }
}