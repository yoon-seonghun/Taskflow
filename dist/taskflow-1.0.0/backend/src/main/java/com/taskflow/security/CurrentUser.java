package com.taskflow.security;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.*;

/**
 * 현재 인증된 사용자 정보 주입 어노테이션
 *
 * 사용 예시:
 * <pre>
 * @GetMapping("/me")
 * public ApiResponse<UserResponse> getMe(@CurrentUser UserPrincipal user) {
 *     return ApiResponse.success(userService.getUser(user.getUserId()));
 * }
 * </pre>
 */
@Target({ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal
public @interface CurrentUser {
}
