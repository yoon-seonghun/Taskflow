package com.taskflow.dto.todo;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Todo 이관 요청 DTO
 */
@Getter
@Setter
public class TodoTransferRequest {

    /**
     * 이관받을 사용자 USERNAME (FK 정책 준수)
     */
    @NotBlank(message = "이관받을 사용자를 선택해주세요")
    private String toUsername;

    /**
     * 이관할 Todo ID 목록 (null이면 전체)
     */
    private List<Long> todoIds;
}
