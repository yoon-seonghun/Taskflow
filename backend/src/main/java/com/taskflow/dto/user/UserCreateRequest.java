package com.taskflow.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 사용자 등록 요청 DTO
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
public class UserCreateRequest {

    /**
     * 로그인 아이디 (영문+숫자, 4~20자)
     */
    @NotBlank(message = "아이디는 필수입니다")
    @Size(min = 4, max = 20, message = "아이디는 4~20자 사이여야 합니다")
    @Pattern(regexp = "^[a-zA-Z0-9]+$", message = "아이디는 영문과 숫자만 사용 가능합니다")
    private String username;

    /**
     * 비밀번호
     */
    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, max = 100, message = "비밀번호는 8자 이상이어야 합니다")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*])[A-Za-z\\d!@#$%^&*]{8,}$",
        message = "비밀번호는 영문 대/소문자, 숫자, 특수문자(!@#$%^&*)를 각각 1개 이상 포함해야 합니다"
    )
    private String password;

    /**
     * 비밀번호 확인
     */
    @NotBlank(message = "비밀번호 확인은 필수입니다")
    private String passwordConfirm;

    /**
     * 사용자 이름
     */
    @NotBlank(message = "이름은 필수입니다")
    @Size(max = 50, message = "이름은 50자를 초과할 수 없습니다")
    private String name;

    /**
     * 이메일 주소
     */
    @Size(max = 100, message = "이메일은 100자를 초과할 수 없습니다")
    @Pattern(regexp = "^$|^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$", message = "올바른 이메일 형식이 아닙니다")
    private String email;

    /**
     * 소속 부서 ID
     */
    private Long departmentId;

    /**
     * 소속 그룹 ID 목록
     */
    private List<Long> groupIds;

    /**
     * 비밀번호 일치 여부 확인
     */
    public boolean isPasswordMatched() {
        return password != null && password.equals(passwordConfirm);
    }
}
