package com.taskflow.dto.board;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/**
 * 보드 공유 추가 요청 DTO
 */
@Getter
@Setter
public class BoardShareRequest {

    /**
     * 공유할 사용자 아이디
     */
    @NotBlank(message = "사용자 아이디는 필수입니다")
    private String username;

    /**
     * 권한 레벨 (VIEW: 조회, EDIT: 편집, FULL: 전체)
     */
    @Pattern(regexp = "^(VIEW|EDIT|FULL)$", message = "권한은 VIEW, EDIT, FULL 중 하나여야 합니다")
    private String permission = "EDIT";  // 기본값: 편집 권한
}
