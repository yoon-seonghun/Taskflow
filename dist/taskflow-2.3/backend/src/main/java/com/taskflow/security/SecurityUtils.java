package com.taskflow.security;

import com.taskflow.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Security 유틸리티
 *
 * 현재 인증된 사용자 정보 조회
 */
public final class SecurityUtils {

    private SecurityUtils() {
        // 유틸리티 클래스
    }

    /**
     * 현재 인증된 사용자 ID 조회
     *
     * @return 사용자 ID
     * @throws BusinessException 인증되지 않은 경우
     */
    public static Long getCurrentUserId() {
        return getOptionalCurrentUserId()
                .orElseThrow(() -> BusinessException.unauthorized("인증이 필요합니다"));
    }

    /**
     * 현재 인증된 사용자 ID 조회 (Optional)
     *
     * @return Optional<사용자 ID>
     */
    public static Optional<Long> getOptionalCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object details = authentication.getDetails();
        if (details instanceof Long) {
            return Optional.of((Long) details);
        }

        // UserPrincipal인 경우
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPrincipal) {
            return Optional.of(((UserPrincipal) principal).getUserId());
        }

        return Optional.empty();
    }

    /**
     * 현재 인증된 사용자명 조회
     *
     * @return 사용자명
     * @throws BusinessException 인증되지 않은 경우
     */
    public static String getCurrentUsername() {
        return getOptionalCurrentUsername()
                .orElseThrow(() -> BusinessException.unauthorized("인증이 필요합니다"));
    }

    /**
     * 현재 인증된 사용자명 조회 (Optional)
     *
     * @return Optional<사용자명>
     */
    public static Optional<String> getOptionalCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        return Optional.ofNullable(authentication.getName());
    }

    /**
     * 현재 사용자가 인증되었는지 확인
     *
     * @return 인증 여부
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated() &&
                !"anonymousUser".equals(authentication.getPrincipal());
    }

    /**
     * 현재 인증 정보 조회
     *
     * @return Authentication 객체 (없으면 null)
     */
    public static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    /**
     * 인증 정보 설정
     *
     * @param authentication 설정할 인증 정보
     */
    public static void setAuthentication(Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * 인증 정보 제거
     */
    public static void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }
}
