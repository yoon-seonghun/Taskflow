package com.taskflow.dto.todo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/**
 * Todo 공유 요청 DTO
 */
@Getter
@Setter
public class TodoShareRequest {

    /**
     * 공유받을 사용자 USERNAME (FK 정책 준수)
     */
    @NotBlank(message = "공유받을 사용자를 선택해주세요")
    private String sharedUsername;

    /**
     * 권한 (VIEW/EDIT)
     */
    @Pattern(regexp = "^(VIEW|EDIT)$", message = "유효하지 않은 권한입니다")
    private String permission = "VIEW";
}
