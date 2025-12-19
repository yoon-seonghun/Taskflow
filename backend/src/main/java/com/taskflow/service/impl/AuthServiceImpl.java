package com.taskflow.service.impl;

import com.taskflow.common.LogMaskUtils;
import com.taskflow.domain.User;
import com.taskflow.dto.auth.LoginRequest;
import com.taskflow.dto.auth.LoginResponse;
import com.taskflow.dto.auth.TokenRefreshResponse;
import com.taskflow.dto.user.UserResponse;
import com.taskflow.exception.BusinessException;
import com.taskflow.mapper.UserMapper;
import com.taskflow.security.JwtTokenProvider;
import com.taskflow.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 서비스 구현
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    @Override
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt: username={}", LogMaskUtils.maskUsername(request.getUsername()));

        // 사용자 조회
        User user = userMapper.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    log.warn("Login failed: user not found - {}", LogMaskUtils.maskUsername(request.getUsername()));
                    return BusinessException.authenticationFailed("아이디 또는 비밀번호가 올바르지 않습니다");
                });

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed: invalid password - {}", LogMaskUtils.maskUsername(request.getUsername()));
            throw BusinessException.authenticationFailed("아이디 또는 비밀번호가 올바르지 않습니다");
        }

        // 활성 사용자 확인
        if (!user.isActive()) {
            log.warn("Login failed: inactive user - {}", LogMaskUtils.maskUsername(request.getUsername()));
            throw BusinessException.authenticationFailed("비활성화된 계정입니다");
        }

        // Access Token 생성
        String accessToken = jwtTokenProvider.createAccessToken(user.getUserId(), user.getUsername());

        log.info("Login successful: userId={}, username={}", user.getUserId(), LogMaskUtils.maskUsername(user.getUsername()));

        return LoginResponse.of(
                accessToken,
                jwtTokenProvider.getAccessTokenValidity(),
                UserResponse.from(user)
        );
    }

    @Override
    public TokenRefreshResponse refresh(String refreshToken) {
        log.debug("Token refresh attempt");

        // Refresh Token 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            log.warn("Token refresh failed: invalid refresh token");
            throw BusinessException.invalidToken("유효하지 않은 Refresh Token입니다");
        }

        // Refresh Token 타입 검증
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            log.warn("Token refresh failed: not a refresh token");
            throw BusinessException.invalidToken("Refresh Token이 아닙니다");
        }

        // 토큰에서 사용자 정보 추출
        Long userId = jwtTokenProvider.getUserId(refreshToken);
        String username = jwtTokenProvider.getUsername(refreshToken);

        // 사용자 존재 및 활성 상태 확인
        User user = userMapper.findById(userId)
                .orElseThrow(() -> BusinessException.userNotFound(userId));

        if (!user.isActive()) {
            log.warn("Token refresh failed: inactive user - userId={}", userId);
            throw BusinessException.authenticationFailed("비활성화된 계정입니다");
        }

        // 새로운 Access Token 생성
        String newAccessToken = jwtTokenProvider.createAccessToken(userId, username);

        log.info("Token refreshed: userId={}", userId);

        return TokenRefreshResponse.of(newAccessToken, jwtTokenProvider.getAccessTokenValidity());
    }

    @Override
    public void logout(Long userId) {
        log.info("Logout: userId={}", userId);
        // 서버 측 세션 없음 (Stateless)
        // 필요 시 Refresh Token 블랙리스트 처리 가능
    }

    @Override
    public String createRefreshToken(Long userId, String username) {
        return jwtTokenProvider.createRefreshToken(userId, username);
    }
}
