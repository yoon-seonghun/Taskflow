package com.taskflow.config;

import com.taskflow.security.Sha256PasswordEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * PasswordEncoder 설정
 *
 * 사용자 관리 모드에 따라 적절한 PasswordEncoder를 생성:
 * - internal 모드: BCryptPasswordEncoder (strength: 설정값)
 * - external 모드: Sha256PasswordEncoder (MySQL sha2() 호환)
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class PasswordEncoderConfig {

    private final UserManagementProperties userManagementProperties;

    /**
     * 모드에 따른 PasswordEncoder 빈 생성
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        if (userManagementProperties.isExternalMode()) {
            log.info("Using SHA256 PasswordEncoder (External DB mode)");
            return new Sha256PasswordEncoder();
        } else {
            int strength = userManagementProperties.getInternal().getBcryptStrength();
            log.info("Using BCrypt PasswordEncoder (Internal mode, strength: {})", strength);
            return new BCryptPasswordEncoder(strength);
        }
    }
}
