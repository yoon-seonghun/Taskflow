package com.taskflow.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

/**
 * Spring Security UserDetails 구현
 *
 * 인증된 사용자 정보를 담는 Principal 객체
 */
@Getter
public class UserPrincipal implements UserDetails {

    private final Long userId;
    private final String username;
    private final String password;
    private final String name;
    private final Long departmentId;
    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(
            Long userId,
            String username,
            String password,
            String name,
            Long departmentId,
            boolean enabled
    ) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.name = name;
        this.departmentId = departmentId;
        this.enabled = enabled;
        this.authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }

    public static UserPrincipal of(
            Long userId,
            String username,
            String password,
            String name,
            Long departmentId,
            String useYn
    ) {
        return new UserPrincipal(
                userId,
                username,
                password,
                name,
                departmentId,
                "Y".equals(useYn)
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
