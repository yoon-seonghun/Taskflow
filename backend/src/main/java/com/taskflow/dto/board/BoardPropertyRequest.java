package com.taskflow.dto.board;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 보드 속성 설정 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardPropertyRequest {

    /**
     * 필수 여부 (Y/N)
     */
    private String requiredYn;

    /**
     * 표시 여부 (Y/N)
     */
    private String visibleYn;

    /**
     * 기본값
     */
    private String defaultValue;

    /**
     * 정렬 순서
     */
    private Integer sortOrder;
}
