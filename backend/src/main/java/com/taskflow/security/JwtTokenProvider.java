package com.taskflow.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JWT 토큰 생성 및 검증
 *
 * 토큰 정책:
 * - Access Token: 유효기간 30분, localStorage 저장
 * - Refresh Token: 유효기간 7일, httpOnly Cookie 저장
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenValidity;
    private final long refreshTokenValidity;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity}") long accessTokenValidity,
            @Value("${jwt.refresh-token-validity}") long refreshTokenValidity
    ) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessTokenValidity = accessTokenValidity;
        this.refreshTokenValidity = refreshTokenValidity;
    }

    /**
     * Access Token 생성
     */
    public String createAccessToken(Long userId, String username) {
        return createToken(userId, username, accessTokenValidity, "ACCESS");
    }

    /**
     * Refresh Token 생성
     */
    public String createRefreshToken(Long userId, String username) {
        return createToken(userId, username, refreshTokenValidity, "REFRESH");
    }

    /**
     * JWT 토큰 생성
     */
    private String createToken(Long userId, String username, long validity, String tokenType) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + validity);

        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("type", tokenType)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 토큰에서 Authentication 객체 생성
     */
    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);
        String username = claims.getSubject();
        Long userId = claims.get("userId", Long.class);

        // UserPrincipal 생성 (JWT에서 추출 가능한 정보만 사용)
        UserPrincipal userPrincipal = new UserPrincipal(
                userId,
                username,
                "",           // password는 인증에 필요없음
                username,     // name은 username으로 대체
                null,         // departmentId
                true          // enabled
        );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userPrincipal, token, userPrincipal.getAuthorities());

        return authentication;
    }

    /**
     * 토큰에서 사용자 ID 추출
     */
    public Long getUserId(String token) {
        Claims claims = parseClaims(token);
        return claims.get("userId", Long.class);
    }

    /**
     * 토큰에서 사용자명 추출
     */
    public String getUsername(String token) {
        Claims claims = parseClaims(token);
        return claims.getSubject();
    }

    /**
     * 토큰 유효성 검증
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (SecurityException | MalformedJwtException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Access Token 검증 (타입 체크 포함)
     */
    public boolean validateAccessToken(String token) {
        try {
            Claims claims = parseClaims(token);
            String tokenType = claims.get("type", String.class);
            return "ACCESS".equals(tokenType);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Refresh Token 검증 (타입 체크 포함)
     */
    public boolean validateRefreshToken(String token) {
        try {
            Claims claims = parseClaims(token);
            String tokenType = claims.get("type", String.class);
            return "REFRESH".equals(tokenType);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 토큰 만료 여부 확인
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseClaims(token);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 토큰 만료 시간 조회
     */
    public Date getExpiration(String token) {
        Claims claims = parseClaims(token);
        return claims.getExpiration();
    }

    /**
     * 토큰 남은 유효 시간 (밀리초)
     */
    public long getRemaining(String token) {
        Date expiration = getExpiration(token);
        return expiration.getTime() - System.currentTimeMillis();
    }

    /**
     * 토큰 Claims 파싱
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Access Token 유효 시간 (밀리초)
     */
    public long getAccessTokenValidity() {
        return accessTokenValidity;
    }

    /**
     * Refresh Token 유효 시간 (밀리초)
     */
    public long getRefreshTokenValidity() {
        return refreshTokenValidity;
    }
}
