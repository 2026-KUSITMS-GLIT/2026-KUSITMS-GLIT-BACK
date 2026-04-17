package com.groute.groute_server.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final String code;
    private final String message;
    private final T data;

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, "200", message, data);
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, "200", null, data);
    }

    public static ApiResponse<Void> ok(String message) {
        return new ApiResponse<>(true, "200", message, null);
    }

    public static <T> ApiResponse<T> created(String message, T data) {
        return new ApiResponse<>(true, "201", message, data);
    }

    public static ApiResponse<Void> created(String message) {
        return new ApiResponse<>(true, "201", message, null);
    }

    public static ApiResponse<Void> fail(String code, String message) {
        return new ApiResponse<>(false, code, message, null);
    }
}
