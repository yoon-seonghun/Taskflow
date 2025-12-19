package com.taskflow.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 인증 필터
 *
 * 모든 요청에서 JWT 토큰을 확인하고 인증 처리
 * Authorization: Bearer {token} 헤더에서 토큰 추출
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // 토큰 추출
        String token = resolveToken(request);

        // 토큰 검증 및 인증 처리
        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            // Access Token 타입 검증
            if (jwtTokenProvider.validateAccessToken(token)) {
                Authentication authentication = jwtTokenProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Set Authentication to security context for '{}', uri: {}",
                        authentication.getName(), request.getRequestURI());
            } else {
                log.warn("Invalid token type for uri: {}", request.getRequestURI());
            }
        } else if (StringUtils.hasText(token)) {
            log.debug("Invalid JWT token for uri: {}", request.getRequestURI());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Request Header에서 토큰 추출
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    /**
     * 필터 적용 제외 경로
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();

        // 인증이 필요 없는 경로
        return path.startsWith("/api/auth/login") ||
               path.startsWith("/api/auth/refresh") ||
               path.startsWith("/api/health") ||
               path.startsWith("/actuator") ||
               path.startsWith("/swagger") ||
               path.startsWith("/v3/api-docs");
    }
}
