package com.taskflow.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 비밀번호 변경 요청 DTO
 *
 * 비밀번호 정책:
 * - 최소 8자 이상
 * - 영문 대문자 1개 이상
 * - 영문 소문자 1개 이상
 * - 숫자 1개 이상
 * - 특수문자 1개 이상 (!@#$%^&*)
 */
@Getter
@Setter
public class PasswordChangeRequest {

    /**
     * 현재 비밀번호
     */
    @NotBlank(message = "현재 비밀번호는 필수입니다")
    private String currentPassword;

    /**
     * 새 비밀번호
     */
    @NotBlank(message = "새 비밀번호는 필수입니다")
    @Size(min = 8, max = 100, message = "비밀번호는 8자 이상이어야 합니다")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*])[A-Za-z\\d!@#$%^&*]{8,}$",
        message = "비밀번호는 영문 대/소문자, 숫자, 특수문자(!@#$%^&*)를 각각 1개 이상 포함해야 합니다"
    )
    private String newPassword;

    /**
     * 새 비밀번호 확인
     */
    @NotBlank(message = "새 비밀번호 확인은 필수입니다")
    private String newPasswordConfirm;

    /**
     * 새 비밀번호 일치 여부 확인
     */
    public boolean isNewPasswordMatched() {
        return newPassword != null && newPassword.equals(newPasswordConfirm);
    }
}
