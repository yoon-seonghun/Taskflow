package com.taskflow.dto.todo;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

/**
 * Todo 공유 권한 수정 요청 DTO
 */
@Getter
@Setter
public class TodoShareUpdateRequest {

    /**
     * 권한 (VIEW/EDIT)
     */
    @Pattern(regexp = "^(VIEW|EDIT)$", message = "유효하지 않은 권한입니다")
    private String permission;
}
