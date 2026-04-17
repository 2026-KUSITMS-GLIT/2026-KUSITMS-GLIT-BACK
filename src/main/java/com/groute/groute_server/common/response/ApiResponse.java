package com.groute.groute_server.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 공통 API 응답 래퍼.
 *
 * <p>모든 API 엔드포인트는 이 클래스를 통해 일관된 형식으로 응답을 반환한다.
 * null 필드는 JSON 직렬화 시 생략된다.</p>
 *
 * <pre>{@code
 * // 데이터 + 메시지
 * ApiResponse.ok("조회 성공", userDto);
 *
 * // 데이터만
 * ApiResponse.ok(userDto);
 *
 * // 메시지만
 * ApiResponse.ok("삭제 완료");
 * }</pre>
 *
 * @param <T> 응답 데이터 타입
 */
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
