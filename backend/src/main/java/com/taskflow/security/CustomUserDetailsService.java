package com.taskflow.security;

import com.taskflow.common.LogMaskUtils;
import com.taskflow.domain.User;
import com.taskflow.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Security UserDetailsService 구현
 *
 * 사용자 인증 시 DB에서 사용자 정보 조회
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("Loading user by username: {}", LogMaskUtils.maskUsername(username));

        User user = userMapper.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User not found: {}", LogMaskUtils.maskUsername(username));
                    return new UsernameNotFoundException("사용자를 찾을 수 없습니다");
                });

        return UserPrincipal.of(
                user.getUserId(),
                user.getUsername(),
                user.getPassword(),
                user.getName(),
                user.getDepartmentId(),
                user.getUseYn()
        );
    }

    /**
     * 사용자 ID로 UserDetails 조회
     */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long userId) {
        log.debug("Loading user by id: {}", userId);

        User user = userMapper.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User not found: {}", userId);
                    return new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userId);
                });

        return UserPrincipal.of(
                user.getUserId(),
                user.getUsername(),
                user.getPassword(),
                user.getName(),
                user.getDepartmentId(),
                user.getUseYn()
        );
    }
}
