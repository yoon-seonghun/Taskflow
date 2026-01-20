package com.taskflow.dto.auth;

import lombok.Builder;
import lombok.Getter;

/**
 * 토큰 갱신 응답 DTO
 */
@Getter
@Builder
public class TokenRefreshResponse {

    /**
     * 새로운 Access Token
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
     * 기본 토큰 타입으로 TokenRefreshResponse 생성
     */
    public static TokenRefreshResponse of(String accessToken, Long expiresIn) {
        return TokenRefreshResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .build();
    }
}
