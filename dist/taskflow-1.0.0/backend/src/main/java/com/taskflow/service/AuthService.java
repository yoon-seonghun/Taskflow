package com.taskflow.service;

import com.taskflow.dto.auth.LoginRequest;
import com.taskflow.dto.auth.LoginResponse;
import com.taskflow.dto.auth.TokenRefreshResponse;

/**
 * 인증 서비스 인터페이스
 */
public interface AuthService {

    /**
     * 로그인
     *
     * @param request 로그인 요청
     * @return 로그인 응답 (Access Token, 사용자 정보)
     */
    LoginResponse login(LoginRequest request);

    /**
     * 토큰 갱신
     *
     * @param refreshToken Refresh Token
     * @return 새로운 Access Token
     */
    TokenRefreshResponse refresh(String refreshToken);

    /**
     * 로그아웃
     *
     * @param userId 사용자 ID
     */
    void logout(Long userId);

    /**
     * Refresh Token 생성 (Cookie 저장용)
     *
     * @param userId 사용자 ID
     * @param username 로그인 아이디
     * @return Refresh Token
     */
    String createRefreshToken(Long userId, String username);
}
