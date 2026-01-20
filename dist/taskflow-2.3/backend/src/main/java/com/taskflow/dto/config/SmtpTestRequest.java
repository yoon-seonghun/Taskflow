package com.taskflow.dto.config;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * SMTP 테스트 요청 DTO
 */
@Getter
@Setter
public class SmtpTestRequest {

    /**
     * 테스트 메일 수신 주소
     */
    @NotBlank(message = "테스트 이메일 주소는 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String testEmail;
}
