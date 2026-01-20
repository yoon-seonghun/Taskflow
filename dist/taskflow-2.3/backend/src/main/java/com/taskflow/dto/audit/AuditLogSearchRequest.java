package com.taskflow.dto.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 감사 로그 검색 요청 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogSearchRequest {

    /**
     * 대상 유형 필터 (BOARD/ITEM/BOARD_SHARE/ITEM_SHARE)
     */
    private String targetType;

    /**
     * 작업 유형 필터 (CREATE/UPDATE/DELETE/TRANSFER/SHARE/UNSHARE)
     */
    private String action;

    /**
     * 시작일
     */
    private LocalDate startDate;

    /**
     * 종료일
     */
    private LocalDate endDate;

    /**
     * 수행자 ID 필터
     */
    private Long actorId;

    /**
     * 페이지 번호 (0부터 시작)
     */
    @Builder.Default
    private Integer page = 0;

    /**
     * 페이지 크기
     */
    @Builder.Default
    private Integer size = 20;

    /**
     * 오프셋 계산
     */
    public int getOffset() {
        return page * size;
    }
}
