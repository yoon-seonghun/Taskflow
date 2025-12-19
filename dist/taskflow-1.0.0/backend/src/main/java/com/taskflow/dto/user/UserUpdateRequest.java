package com.taskflow.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 사용자 수정 요청 DTO
 */
@Getter
@Setter
public class UserUpdateRequest {

    /**
     * 사용자 이름
     */
    @NotBlank(message = "이름은 필수입니다")
    @Size(max = 50, message = "이름은 50자를 초과할 수 없습니다")
    private String name;

    /**
     * 새 비밀번호 (관리자가 비밀번호 변경 시 사용, 선택)
     */
    @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다")
    private String password;

    /**
     * 소속 부서 ID
     */
    private Long departmentId;

    /**
     * 사용 여부 (Y/N)
     */
    private String useYn;

    /**
     * 소속 그룹 ID 목록
     */
    private List<Long> groupIds;
}
