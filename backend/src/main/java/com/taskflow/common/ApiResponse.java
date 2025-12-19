package com.taskflow.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

/**
 * 공통 API 응답 래퍼
 *
 * 성공 응답:
 * {
 *   "success": true,
 *   "data": { ... },
 *   "message": null
 * }
 *
 * 실패 응답:
 * {
 *   "success": false,
 *   "data": null,
 *   "message": "에러 메시지"
 * }
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final String message;

    private ApiResponse(boolean success, T data, String message) {
        this.success = success;
        this.data = data;
        this.message = message;
    }

    /**
     * 데이터와 함께 성공 응답 생성
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    /**
     * 데이터 없이 성공 응답 생성
     */
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(true, null, null);
    }

    /**
     * 성공 메시지와 함께 응답 생성
     */
    public static <T> ApiResponse<T> successWithMessage(String message) {
        return new ApiResponse<>(true, null, message);
    }

    /**
     * 데이터와 메시지 함께 성공 응답 생성
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, data, message);
    }

    /**
     * 에러 응답 생성
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message);
    }
}
