package com.taskflow.dto.board;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 보드 삭제 요청 DTO
 * 미완료 업무가 있는 경우 이관 정보 포함
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardDeleteRequest {

    /**
     * 이관받을 사용자 ID (미완료 업무 존재 시 필수)
     * - 컨트롤러에서는 targetUserId로, 서비스에서는 transferToUserId로도 사용
     */
    private Long targetUserId;

    /**
     * 이관 사유 (선택)
     */
    private String transferReason;

    /**
     * 이관할 업무 ID 목록 (지정하지 않으면 전체 미완료 업무)
     */
    private List<Long> itemIds;

    /**
     * 미완료 업무가 있어도 강제 삭제 여부
     */
    private boolean forceDelete;

    // Alias getter for transferToUserId
    public Long getTransferToUserId() {
        return targetUserId;
    }

    public void setTransferToUserId(Long userId) {
        this.targetUserId = userId;
    }
}
