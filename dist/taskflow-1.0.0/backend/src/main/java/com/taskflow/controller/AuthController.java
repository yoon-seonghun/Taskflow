package com.taskflow.controller;

import com.taskflow.common.ApiResponse;
import com.taskflow.common.LogMaskUtils;
import com.taskflow.dto.auth.LoginRequest;
import com.taskflow.dto.auth.LoginResponse;
import com.taskflow.dto.auth.TokenRefreshResponse;
import com.taskflow.dto.user.UserResponse;
import com.taskflow.security.CurrentUser;
import com.taskflow.security.JwtTokenProvider;
import com.taskflow.security.SecurityUtils;
import com.taskflow.security.UserPrincipal;
import com.taskflow.service.AuthService;
import com.taskflow.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 인증 컨트롤러
 *
 * API:
 * - POST /api/auth/login - 로그인
 * - POST /api/auth/logout - 로그아웃
 * - POST /api/auth/refresh - 토큰 갱신
 * - GET /api/auth/me - 내 정보 조회
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    @Value("${jwt.refresh-token-validity}")
    private long refreshTokenValidity;

    @Value("${cookie.secure:false}")
    private boolean cookieSecure;

    /**
     * 로그인
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        log.info("Login request: username={}", LogMaskUtils.maskUsername(request.getUsername()));

        // 로그인 처리
        LoginResponse loginResponse = authService.login(request);

        // Refresh Token 생성 및 Cookie 설정
        String refreshToken = authService.createRefreshToken(
                loginResponse.getUser().getUserId(),
                loginResponse.getUser().getUsername()
        );
        setRefreshTokenCookie(response, refreshToken);

        return ResponseEntity.ok(ApiResponse.success(loginResponse));
    }

    /**
     * 로그아웃
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {
        // 현재 사용자 ID 조회 (인증된 경우)
        SecurityUtils.getOptionalCurrentUserId()
                .ifPresent(authService::logout);

        // Refresh Token Cookie 삭제
        clearRefreshTokenCookie(response);

        log.info("Logout successful");
        return ResponseEntity.ok(ApiResponse.successWithMessage("로그아웃 되었습니다"));
    }

    /**
     * 토큰 갱신
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refresh(HttpServletRequest request) {
        // Cookie에서 Refresh Token 추출
        String refreshToken = extractRefreshTokenFromCookie(request);

        if (refreshToken == null) {
            log.warn("Refresh token not found in cookie");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Refresh Token이 없습니다"));
        }

        // 토큰 갱신
        TokenRefreshResponse tokenResponse = authService.refresh(refreshToken);

        return ResponseEntity.ok(ApiResponse.success(tokenResponse));
    }

    /**
     * 내 정보 조회
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMe() {
        Long userId = SecurityUtils.getCurrentUserId();
        UserResponse user = userService.getUser(userId);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    // =============================================
    // Private Methods
    // =============================================

    /**
     * Refresh Token Cookie 설정
     */
    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure); // 환경변수로 설정 (프로덕션: true)
        cookie.setPath("/api/auth");
        cookie.setMaxAge((int) (refreshTokenValidity / 1000)); // 초 단위
        // SameSite 속성 설정 (Spring Boot 2.6+ 에서 지원)
        String secureFlag = cookieSecure ? "; Secure" : "";
        response.addHeader("Set-Cookie",
                String.format("%s=%s; Path=/api/auth; HttpOnly; Max-Age=%d; SameSite=Strict%s",
                        REFRESH_TOKEN_COOKIE_NAME, refreshToken, (int) (refreshTokenValidity / 1000), secureFlag));
    }

    /**
     * Refresh Token Cookie 삭제
     */
    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure); // 환경변수로 설정 (프로덕션: true)
        cookie.setPath("/api/auth");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    /**
     * Cookie에서 Refresh Token 추출
     */
    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (REFRESH_TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
