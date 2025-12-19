package com.taskflow.dto.item;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 아이템 검색/필터 요청 DTO
 */
@Getter
@Setter
public class ItemSearchRequest {

    /**
     * 검색 키워드 (제목, 내용에서 검색)
     */
    private String keyword;

    /**
     * 상태 필터
     */
    private String status;

    /**
     * 우선순위 필터
     */
    private String priority;

    /**
     * 담당자 ID 필터
     */
    private Long assigneeId;

    /**
     * 그룹 ID 필터
     */
    private Long groupId;

    /**
     * 카테고리 ID 필터
     */
    private Long categoryId;

    /**
     * 시작일 (생성일 기준)
     */
    private LocalDate startDate;

    /**
     * 종료일 (생성일 기준)
     */
    private LocalDate endDate;

    /**
     * 완료된 아이템 포함 여부
     */
    private Boolean includeCompleted = false;

    /**
     * 삭제된 아이템 포함 여부
     */
    private Boolean includeDeleted = false;

    /**
     * 페이지 번호 (0부터 시작)
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
     * 정렬 방향 (asc, desc)
     */
    private String sortDirection = "desc";

    /**
     * 오프셋 계산
     */
    public int getOffset() {
        return page * size;
    }

    /**
     * 정렬 SQL 문자열 반환
     */
    public String getOrderBy() {
        String field = switch (sortField) {
            case "title", "content" -> "i.CONTENT";
            case "status" -> "i.STATUS";
            case "priority" -> "i.PRIORITY";
            case "assigneeId" -> "i.ASSIGNEE_ID";
            case "groupId" -> "i.GROUP_ID";
            case "categoryId" -> "i.CATEGORY_ID";
            case "startTime" -> "i.START_TIME";
            case "endTime", "dueDate" -> "i.END_TIME";
            case "createdAt" -> "i.CREATED_AT";
            case "updatedAt" -> "i.UPDATED_AT";
            default -> "i.CREATED_AT";
        };

        String direction = "desc".equalsIgnoreCase(sortDirection) ? "DESC" : "ASC";
        return field + " " + direction;
    }
}
