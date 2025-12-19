package com.taskflow.config;

import com.taskflow.security.JwtAccessDeniedHandler;
import com.taskflow.security.JwtAuthenticationEntryPoint;
import com.taskflow.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 설정
 *
 * 인증 방식: JWT (JSON Web Token)
 * - Access Token: 유효기간 30분, Authorization 헤더
 * - Refresh Token: 유효기간 7일, httpOnly Cookie
 *
 * 보안 정책:
 * - CSRF: 비활성화 (JWT 사용)
 * - Session: STATELESS
 * - BCrypt: strength 10
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final RateLimitingFilter rateLimitingFilter;

    /**
     * 인증 없이 접근 가능한 경로
     */
    private static final String[] PUBLIC_URLS = {
            "/api/auth/login",
            "/api/auth/refresh",
            "/api/health",
            "/actuator/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/error"
    };

    /**
     * SSE 엔드포인트 (인증 필요하지만 특별 처리)
     */
    private static final String[] SSE_URLS = {
            "/api/sse/**"
    };

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF 비활성화 (JWT 사용)
            .csrf(AbstractHttpConfigurer::disable)

            // CORS 설정 (CorsConfig 참조)
            .cors(cors -> {})

            // 세션 사용 안함 (Stateless)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 예외 처리
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(jwtAccessDeniedHandler))

            // URL별 접근 권한 설정
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_URLS).permitAll()
                .requestMatchers(SSE_URLS).authenticated()
                .anyRequest().authenticated()
            )

            // Rate Limiting 필터 추가 (JWT 필터보다 먼저 실행)
            .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)

            // JWT 인증 필터 추가
            .addFilterAfter(jwtAuthenticationFilter, RateLimitingFilter.class);

        return http.build();
    }

    /**
     * AuthenticationManager 빈 등록
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration
    ) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * BCrypt 패스워드 인코더
     * strength: 10 (기본값)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
