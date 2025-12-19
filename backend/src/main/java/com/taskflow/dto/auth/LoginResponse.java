package com.taskflow.dto.auth;

import com.taskflow.dto.user.UserResponse;
import lombok.Builder;
import lombok.Getter;

/**
 * 로그인 응답 DTO
 */
@Getter
@Builder
public class LoginResponse {

    /**
     * Access Token
     */
    private String accessToken;

    /**
     * Token 타입 (Bearer)
     */
    private String tokenType;

    /**
     * Access Token 만료 시간 (밀리초)
     */
    private Long expiresIn;

    /**
     * 사용자 정보
     */
    private UserResponse user;

    /**
     * 기본 토큰 타입으로 LoginResponse 생성
     */
    public static LoginResponse of(String accessToken, Long expiresIn, UserResponse user) {
        return LoginResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .user(user)
                .build();
    }
}
