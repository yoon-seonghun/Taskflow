package com.taskflow.dto.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 감사 로그 페이징 응답 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogPageResponse {

    /**
     * 로그 목록
     */
    private List<AuditLogResponse> content;

    /**
     * 전체 항목 수
     */
    private Long totalElements;

    /**
     * 전체 페이지 수
     */
    private Integer totalPages;

    /**
     * 현재 페이지 번호
     */
    private Integer number;

    /**
     * 페이지 크기
     */
    private Integer size;

    /**
     * 첫 페이지 여부
     */
    public boolean isFirst() {
        return number == 0;
    }

    /**
     * 마지막 페이지 여부
     */
    public boolean isLast() {
        return number >= totalPages - 1;
    }
}
