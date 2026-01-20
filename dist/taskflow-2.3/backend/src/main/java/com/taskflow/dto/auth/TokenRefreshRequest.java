package com.taskflow.dto.auth;

import lombok.Getter;
import lombok.Setter;

/**
 * 토큰 갱신 요청 DTO
 *
 * Refresh Token은 httpOnly Cookie로 전송되므로
 * 별도 필드가 필요하지 않음 (Cookie에서 추출)
 */
@Getter
@Setter
public class TokenRefreshRequest {

    /**
     * Refresh Token (Cookie로 전송되지 않는 경우 대비)
     */
    private String refreshToken;
}
