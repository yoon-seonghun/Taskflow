package com.taskflow.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * 로그인 요청 DTO
 */
@Getter
@Setter
public class LoginRequest {

    /**
     * 로그인 아이디
     */
    @NotBlank(message = "아이디를 입력해주세요")
    private String username;

    /**
     * 비밀번호
     */
    @NotBlank(message = "비밀번호를 입력해주세요")
    private String password;

    /**
     * username getter (Lombok 호환성)
     */
    public String getUsername() {
        return username;
    }

    /**
     * username setter (Lombok 호환성)
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * password getter (Lombok 호환성)
     */
    public String getPassword() {
        return password;
    }

    /**
     * password setter (Lombok 호환성)
     */
    public void setPassword(String password) {
        this.password = password;
    }
}
