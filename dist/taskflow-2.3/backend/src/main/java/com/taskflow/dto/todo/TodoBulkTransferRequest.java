package com.taskflow.dto.todo;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Todo 일괄 이관 요청 DTO (사용자 삭제 시)
 */
@Getter
@Setter
public class TodoBulkTransferRequest {

    /**
     * 이관할 사용자 ID (삭제될 사용자)
     */
    @NotNull(message = "이관 출처 사용자 ID는 필수입니다")
    private Long fromUserId;

    /**
     * 이관받을 사용자 ID
     */
    @NotNull(message = "이관받을 사용자 ID는 필수입니다")
    private Long toUserId;

    /**
     * 이관할 Todo ID 목록 (null이면 전체)
     */
    private List<Long> todoIds;
}
