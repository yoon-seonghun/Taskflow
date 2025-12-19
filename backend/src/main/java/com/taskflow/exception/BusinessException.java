package com.taskflow.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 비즈니스 예외
 *
 * HTTP 상태 코드:
 * - 400 Bad Request: 유효성 검증 실패
 * - 401 Unauthorized: 인증 필요
 * - 403 Forbidden: 권한 없음
 * - 404 Not Found: 리소스 없음
 * - 409 Conflict: 충돌 (동시 수정)
 */
@Getter
public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public BusinessException(String message) {
        super(message);
        this.status = HttpStatus.BAD_REQUEST;
        this.errorCode = "BAD_REQUEST";
    }

    public BusinessException(String message, HttpStatus status) {
        super(message);
        this.status = status;
        this.errorCode = status.name();
    }

    public BusinessException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    // =============================================
    // 400 Bad Request
    // =============================================

    public static BusinessException badRequest(String message) {
        return new BusinessException(message, HttpStatus.BAD_REQUEST, "BAD_REQUEST");
    }

    public static BusinessException invalidInput(String message) {
        return new BusinessException(message, HttpStatus.BAD_REQUEST, "INVALID_INPUT");
    }

    public static BusinessException invalidPassword(String message) {
        return new BusinessException(message, HttpStatus.BAD_REQUEST, "INVALID_PASSWORD");
    }

    // =============================================
    // 401 Unauthorized
    // =============================================

    public static BusinessException unauthorized(String message) {
        return new BusinessException(message, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
    }

    public static BusinessException invalidToken(String message) {
        return new BusinessException(message, HttpStatus.UNAUTHORIZED, "INVALID_TOKEN");
    }

    public static BusinessException expiredToken(String message) {
        return new BusinessException(message, HttpStatus.UNAUTHORIZED, "EXPIRED_TOKEN");
    }

    public static BusinessException authenticationFailed(String message) {
        return new BusinessException(message, HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED");
    }

    // =============================================
    // 403 Forbidden
    // =============================================

    public static BusinessException forbidden(String message) {
        return new BusinessException(message, HttpStatus.FORBIDDEN, "FORBIDDEN");
    }

    public static BusinessException accessDenied(String message) {
        return new BusinessException(message, HttpStatus.FORBIDDEN, "ACCESS_DENIED");
    }

    // =============================================
    // 404 Not Found
    // =============================================

    public static BusinessException notFound(String message) {
        return new BusinessException(message, HttpStatus.NOT_FOUND, "NOT_FOUND");
    }

    public static BusinessException userNotFound(Long userId) {
        return new BusinessException("사용자를 찾을 수 없습니다. ID: " + userId, HttpStatus.NOT_FOUND, "USER_NOT_FOUND");
    }

    public static BusinessException boardNotFound(Long boardId) {
        return new BusinessException("보드를 찾을 수 없습니다. ID: " + boardId, HttpStatus.NOT_FOUND, "BOARD_NOT_FOUND");
    }

    public static BusinessException itemNotFound(Long itemId) {
        return new BusinessException("아이템을 찾을 수 없습니다. ID: " + itemId, HttpStatus.NOT_FOUND, "ITEM_NOT_FOUND");
    }

    public static BusinessException departmentNotFound(Long departmentId) {
        return new BusinessException("부서를 찾을 수 없습니다. ID: " + departmentId, HttpStatus.NOT_FOUND, "DEPARTMENT_NOT_FOUND");
    }

    public static BusinessException groupNotFound(Long groupId) {
        return new BusinessException("그룹을 찾을 수 없습니다. ID: " + groupId, HttpStatus.NOT_FOUND, "GROUP_NOT_FOUND");
    }

    public static BusinessException propertyNotFound(Long propertyId) {
        return new BusinessException("속성을 찾을 수 없습니다. ID: " + propertyId, HttpStatus.NOT_FOUND, "PROPERTY_NOT_FOUND");
    }

    public static BusinessException optionNotFound(Long optionId) {
        return new BusinessException("옵션을 찾을 수 없습니다. ID: " + optionId, HttpStatus.NOT_FOUND, "OPTION_NOT_FOUND");
    }

    public static BusinessException commentNotFound(Long commentId) {
        return new BusinessException("댓글을 찾을 수 없습니다. ID: " + commentId, HttpStatus.NOT_FOUND, "COMMENT_NOT_FOUND");
    }

    public static BusinessException templateNotFound(Long templateId) {
        return new BusinessException("템플릿을 찾을 수 없습니다. ID: " + templateId, HttpStatus.NOT_FOUND, "TEMPLATE_NOT_FOUND");
    }

    // =============================================
    // 409 Conflict
    // =============================================

    public static BusinessException conflict(String message) {
        return new BusinessException(message, HttpStatus.CONFLICT, "CONFLICT");
    }

    public static BusinessException duplicateUsername(String username) {
        return new BusinessException("이미 사용 중인 아이디입니다: " + username, HttpStatus.CONFLICT, "DUPLICATE_USERNAME");
    }

    public static BusinessException duplicateCode(String code) {
        return new BusinessException("이미 사용 중인 코드입니다: " + code, HttpStatus.CONFLICT, "DUPLICATE_CODE");
    }

    public static BusinessException dataInUse(String message) {
        return new BusinessException(message, HttpStatus.CONFLICT, "DATA_IN_USE");
    }
}
