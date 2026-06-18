package com.library.management.exception;

public interface CommonError {
    String getMessage();
    Integer getCode();
    String getCause();
}