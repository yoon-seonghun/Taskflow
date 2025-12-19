package com.taskflow.dto.history;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 작업 등록 이력 검색 요청 DTO
 */
@Getter
@Setter
public class TemplateHistorySearchRequest {

    /**
     * 상태 (ACTIVE, INACTIVE)
     */
    private String status;

    /**
     * 등록자 ID
     */
    private Long createdBy;

    /**
     * 검색 시작일
     */
    private LocalDate startDate;

    /**
     * 검색 종료일
     */
    private LocalDate endDate;

    /**
     * 키워드 (내용 검색)
     */
    private String keyword;

    /**
     * 페이지 번호
     */
    private Integer page = 0;

    /**
     * 페이지 크기
     */
    private Integer size = 20;

    /**
     * 정렬 필드
     */
    private String sortField = "createdAt";

    /**
     * 정렬 방향
     */
    private String sortDirection = "desc";

    /**
     * 오프셋 계산
     */
    public int getOffset() {
        return page * size;
    }

    /**
     * ORDER BY 절 생성
     */
    public String getOrderBy() {
        String field = switch (sortField) {
            case "content" -> "t.CONTENT";
            case "status" -> "t.STATUS";
            case "createdByName" -> "CREATED_BY_NAME";
            case "createdAt" -> "t.CREATED_AT";
            case "updatedAt" -> "t.UPDATED_AT";
            default -> "t.CREATED_AT";
        };
        // SQL Injection 방지: sortDirection 화이트리스트 검증
        String safeDirection = "desc".equalsIgnoreCase(sortDirection) ? "DESC" :
                              "asc".equalsIgnoreCase(sortDirection) ? "ASC" : "DESC";
        return field + " " + safeDirection;
    }
}
