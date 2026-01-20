package com.taskflow.dto.todo;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Todo 순서 변경 요청 DTO
 */
@Getter
@Setter
public class TodoReorderRequest {

    /**
     * Todo ID 순서 목록
     */
    @NotEmpty(message = "Todo ID 목록은 필수입니다")
    private List<Long> todoIds;
}
