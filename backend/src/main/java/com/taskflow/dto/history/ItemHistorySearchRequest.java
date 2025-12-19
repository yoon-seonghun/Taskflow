package com.taskflow.dto.history;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 작업 처리 이력 검색 요청 DTO
 */
@Getter
@Setter
public class ItemHistorySearchRequest {

    /**
     * 보드 ID
     */
    private Long boardId;

    /**
     * 작업 결과 (COMPLETED, DELETED)
     */
    private String result;

    /**
     * 작업자 ID
     */
    private Long workerId;

    /**
     * 검색 시작일
     */
    private LocalDate startDate;

    /**
     * 검색 종료일
     */
    private LocalDate endDate;

    /**
     * 키워드 (제목 검색)
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
    private String sortField = "completedAt";

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
     * ORDER BY 절 생성 (SQL Injection 방어)
     */
    public String getOrderBy() {
        // 정렬 필드 화이트리스트 검증
        String field = switch (sortField) {
            case "title", "content" -> "i.CONTENT";
            case "result" -> "RESULT";
            case "workerName" -> "WORKER_NAME";
            case "createdAt" -> "i.CREATED_AT";
            case "startTime" -> "i.START_TIME";
            case "completedAt", "endTime" -> "i.END_TIME";
            case "updatedAt" -> "i.UPDATED_AT";
            case "deletedAt" -> "i.DELETED_AT";
            default -> "COALESCE(i.END_TIME, i.DELETED_AT)";
        };

        // 정렬 방향 검증 (ASC/DESC만 허용)
        String safeDirection = "desc".equalsIgnoreCase(sortDirection) ? "DESC" :
                              "asc".equalsIgnoreCase(sortDirection) ? "ASC" : "DESC";

        return field + " " + safeDirection;
    }
}
