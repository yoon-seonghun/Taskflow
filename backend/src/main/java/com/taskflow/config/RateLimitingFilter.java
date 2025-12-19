package com.taskflow.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Rate Limiting Filter
 *
 * 브루트포스 공격 방어를 위한 요청 속도 제한
 * - 로그인 API: IP당 분당 5회
 * - 일반 API: IP당 분당 100회
 */
@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    // IP별 요청 카운터 저장소
    private final Map<String, RateLimitEntry> rateLimitMap = new ConcurrentHashMap<>();

    // 로그인 API Rate Limit 설정
    private static final int LOGIN_RATE_LIMIT = 5;           // 분당 5회
    private static final long LOGIN_WINDOW_MS = 60_000;      // 1분

    // 일반 API Rate Limit 설정
    private static final int GENERAL_RATE_LIMIT = 100;       // 분당 100회
    private static final long GENERAL_WINDOW_MS = 60_000;    // 1분

    // Rate Limit 만료 시간 (1시간 후 자동 정리)
    private static final long ENTRY_EXPIRY_MS = 3600_000;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String clientIp = getClientIp(request);
        String requestUri = request.getRequestURI();

        // Rate Limit 적용 대상 확인
        boolean isLoginApi = requestUri.equals("/api/auth/login");
        boolean isAuthApi = requestUri.startsWith("/api/auth/");

        // 로그인 API에 대한 엄격한 Rate Limiting
        if (isLoginApi) {
            String loginKey = "LOGIN:" + clientIp;
            if (!checkRateLimit(loginKey, LOGIN_RATE_LIMIT, LOGIN_WINDOW_MS)) {
                log.warn("Rate limit exceeded for login API. IP: {}", clientIp);
                sendRateLimitResponse(response, "로그인 시도 횟수가 초과되었습니다. 1분 후 다시 시도해주세요.");
                return;
            }
        }

        // 일반 API에 대한 Rate Limiting (인증 API 제외)
        if (!isAuthApi) {
            String generalKey = "GENERAL:" + clientIp;
            if (!checkRateLimit(generalKey, GENERAL_RATE_LIMIT, GENERAL_WINDOW_MS)) {
                log.warn("Rate limit exceeded for general API. IP: {}, URI: {}", clientIp, requestUri);
                sendRateLimitResponse(response, "요청 횟수가 초과되었습니다. 잠시 후 다시 시도해주세요.");
                return;
            }
        }

        // 오래된 엔트리 정리 (주기적)
        cleanupExpiredEntries();

        filterChain.doFilter(request, response);
    }

    /**
     * Rate Limit 체크
     * @return true: 허용, false: 거부
     */
    private boolean checkRateLimit(String key, int limit, long windowMs) {
        long now = System.currentTimeMillis();

        RateLimitEntry entry = rateLimitMap.compute(key, (k, existing) -> {
            if (existing == null || now - existing.windowStart > windowMs) {
                // 새 윈도우 시작
                return new RateLimitEntry(now, new AtomicInteger(1));
            } else {
                // 기존 윈도우에 카운트 증가
                existing.count.incrementAndGet();
                return existing;
            }
        });

        return entry.count.get() <= limit;
    }

    /**
     * Rate Limit 초과 응답 전송
     */
    private void sendRateLimitResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(String.format(
                "{\"success\":false,\"data\":null,\"message\":\"%s\"}", message
        ));
    }

    /**
     * 클라이언트 IP 추출 (프록시 환경 고려)
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For에 여러 IP가 있는 경우 첫 번째(원본 클라이언트) 사용
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 오래된 엔트리 정리 (메모리 누수 방지)
     */
    private void cleanupExpiredEntries() {
        long now = System.currentTimeMillis();
        // 1000개 이상이거나 마지막 정리 후 5분 경과 시 정리
        if (rateLimitMap.size() > 1000) {
            rateLimitMap.entrySet().removeIf(entry ->
                    now - entry.getValue().windowStart > ENTRY_EXPIRY_MS
            );
        }
    }

    /**
     * Rate Limit 엔트리
     */
    private static class RateLimitEntry {
        final long windowStart;
        final AtomicInteger count;

        RateLimitEntry(long windowStart, AtomicInteger count) {
            this.windowStart = windowStart;
            this.count = count;
        }
    }
}
