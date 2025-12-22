package com.taskflow.dto.item;

import lombok.Getter;
import lombok.Setter;

/**
 * 업무 이관 요청 DTO
 */
@Getter
@Setter
public class ItemTransferRequest {

    /**
     * 이관 대상 보드 ID
     */
    private Long targetBoardId;

    /**
     * 이관 대상 사용자 ID (사용자의 기본 보드로 이관)
     */
    private Long targetUserId;

    /**
     * 이관 사유 (선택)
     */
    private String reason;
}
