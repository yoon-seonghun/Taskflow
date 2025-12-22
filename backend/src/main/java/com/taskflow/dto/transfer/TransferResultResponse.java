package com.taskflow.dto.transfer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 업무 이관 결과 응답 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferResultResponse {

    /**
     * 이관된 업무 수
     */
    private Integer transferredCount;

    /**
     * 새로 생성된 이관 보드 ID
     */
    private Long newBoardId;

    /**
     * 새로 생성된 이관 보드명
     */
    private String newBoardName;

    /**
     * 결과 메시지
     */
    private String message;
}
