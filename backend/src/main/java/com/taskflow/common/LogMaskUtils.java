package com.taskflow.common;

/**
 * 로그 마스킹 유틸리티
 *
 * 민감 정보를 로그에 출력할 때 마스킹 처리
 */
public final class LogMaskUtils {

    private LogMaskUtils() {
        // 유틸리티 클래스
    }

    /**
     * 사용자명 마스킹
     *
     * 예: "admin" -> "ad***"
     *     "user123" -> "us*****"
     *     "ab" -> "a*"
     *     "a" -> "*"
     */
    public static String maskUsername(String username) {
        if (username == null || username.isEmpty()) {
            return "***";
        }

        int length = username.length();
        if (length == 1) {
            return "*";
        } else if (length == 2) {
            return username.charAt(0) + "*";
        } else {
            // 앞 2글자만 표시, 나머지는 * 처리
            return username.substring(0, 2) + "*".repeat(length - 2);
        }
    }

    /**
     * 이메일 마스킹
     *
     * 예: "user@example.com" -> "us**@example.com"
     */
    public static String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return "***";
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return maskUsername(email);
        }

        String localPart = email.substring(0, atIndex);
        String domain = email.substring(atIndex);

        return maskUsername(localPart) + domain;
    }

    /**
     * 전화번호 마스킹
     *
     * 예: "010-1234-5678" -> "010-****-5678"
     */
    public static String maskPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return "***";
        }

        // 숫자만 추출
        String digits = phoneNumber.replaceAll("[^0-9]", "");

        if (digits.length() < 4) {
            return "***";
        }

        // 마지막 4자리만 표시
        int length = digits.length();
        return "*".repeat(length - 4) + digits.substring(length - 4);
    }
}
