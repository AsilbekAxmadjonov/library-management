package com.library.management.dto.response;

import com.library.management.exception.CommonError;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaseResponse<T> {

    private T data;
    private Boolean success;
    private Error error;
    private LocalDateTime timestamp;

    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(data, true, null, LocalDateTime.now());
    }

    public static <T> BaseResponse<T> fail(String message, Integer code, String cause, List<Error.Field> fields) {
        BaseResponse<T> result = new BaseResponse<>();
        result.error = new Error(code, message, cause, fields);
        result.success = false;
        result.timestamp = LocalDateTime.now();
        return result;
    }

    public static <T> BaseResponse<T> fail(CommonError error) {
        return fail(error.getMessage(), error.getCode(), error.getCause(), null);
    }

    public static <T> BaseResponse<T> fail(CommonError error, List<Error.Field> fields) {
        return fail(error.getMessage(), error.getCode(), error.getCause(), fields);
    }

    public static <T> BaseResponse<T> fail(Error error) {
        return fail(error.getMessage(), error.getCode(), error.getCause(), null);
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Error {
        private Integer code;
        private String message;
        private String cause;
        private List<Field> fields;

        public record Field(String key, String value) {}
    }
}