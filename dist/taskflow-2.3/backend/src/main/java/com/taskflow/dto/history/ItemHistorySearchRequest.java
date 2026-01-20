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
     * 작업자 ID (레거시 호환)
     */
    private Long workerId;

    /**
     * 작업자 USERNAME
     */
    private String workerUsername;

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
            case "requestDate" -> "i.REQUEST_DATE";
            case "completedAt" -> "i.UPDATED_AT";
            case "updatedAt" -> "i.UPDATED_AT";
            case "deletedAt" -> "i.DELETED_AT";
            default -> "COALESCE(i.UPDATED_AT, i.DELETED_AT)";
        };

        // 정렬 방향 검증 (ASC/DESC만 허용)
        String safeDirection = "desc".equalsIgnoreCase(sortDirection) ? "DESC" :
                              "asc".equalsIgnoreCase(sortDirection) ? "ASC" : "DESC";

        return field + " " + safeDirection;
    }

    // ============================================
    // Lombok 호환성을 위한 명시적 getter/setter
    // ============================================

    public Long getBoardId() {
        return boardId;
    }

    public void setBoardId(Long boardId) {
        this.boardId = boardId;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public Long getWorkerId() {
        return workerId;
    }

    public void setWorkerId(Long workerId) {
        this.workerId = workerId;
    }

    public String getWorkerUsername() {
        return workerUsername;
    }

    public void setWorkerUsername(String workerUsername) {
        this.workerUsername = workerUsername;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getSortField() {
        return sortField;
    }

    public void setSortField(String sortField) {
        this.sortField = sortField;
    }

    public String getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(String sortDirection) {
        this.sortDirection = sortDirection;
    }
}
