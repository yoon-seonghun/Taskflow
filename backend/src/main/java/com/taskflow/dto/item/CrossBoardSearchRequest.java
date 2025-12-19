package com.taskflow.dto.item;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Set;

/**
 * Cross-board 아이템 검색/필터 요청 DTO
 * - 여러 보드에 걸친 아이템 조회에 사용
 * - 지연 업무, 보류 업무 등 전체 조회용
 */
@Getter
@Setter
public class CrossBoardSearchRequest {

    /**
     * 허용된 정렬 필드 목록 (SQL 인젝션 방지)
     */
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "title", "content", "status", "priority", "assigneeId", "groupId", "categoryId",
            "startTime", "endTime", "dueDate", "overdueDays",
            "pendingDays", "createdAt", "updatedAt", "boardName"
    );

    /**
     * 검색 키워드 (제목, 내용에서 검색)
     */
    private String keyword;

    /**
     * 상태 필터 (NOT_STARTED, IN_PROGRESS, PENDING 등)
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
     * 보드 ID 필터 (특정 보드만 조회 시)
     */
    private Long boardId;

    /**
     * 지연 업무만 조회 (DUE_DATE < TODAY)
     */
    private Boolean overdueOnly = false;

    /**
     * 시작일 (생성일 기준)
     */
    private LocalDate startDate;

    /**
     * 종료일 (생성일 기준)
     */
    private LocalDate endDate;

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
    private String sortField = "endTime";

    /**
     * 정렬 방향 (asc, desc)
     */
    private String sortDirection = "asc";

    /**
     * 오프셋 계산 (NPE 방지)
     */
    public int getOffset() {
        int safePage = (page != null) ? page : 0;
        int safeSize = (size != null) ? size : 20;
        return safePage * safeSize;
    }

    /**
     * 안전한 페이지 번호 반환 (NPE 방지)
     */
    public int getSafePage() {
        return (page != null) ? page : 0;
    }

    /**
     * 안전한 페이지 크기 반환 (NPE 방지)
     */
    public int getSafeSize() {
        return (size != null) ? size : 20;
    }

    /**
     * 정렬 SQL 문자열 반환 (SQL 인젝션 방지)
     */
    public String getOrderBy() {
        // 정렬 필드 화이트리스트 검증
        String safeSortField = (sortField != null && ALLOWED_SORT_FIELDS.contains(sortField))
                ? sortField
                : "endTime";

        // 정렬 방향 엄격한 검증
        String safeDirection;
        if ("desc".equalsIgnoreCase(sortDirection)) {
            safeDirection = "DESC";
        } else if ("asc".equalsIgnoreCase(sortDirection)) {
            safeDirection = "ASC";
        } else {
            safeDirection = "ASC";
        }

        String field = switch (safeSortField) {
            case "title", "content" -> "i.CONTENT";
            case "status" -> "i.STATUS";
            case "priority" -> "FIELD(i.PRIORITY, 'URGENT', 'HIGH', 'NORMAL', 'LOW')";
            case "assigneeId" -> "i.ASSIGNEE_ID";
            case "groupId" -> "i.GROUP_ID";
            case "categoryId" -> "i.CATEGORY_ID";
            case "startTime" -> "i.START_TIME";
            case "endTime", "dueDate" -> "i.END_TIME";
            case "overdueDays" -> "DATEDIFF(CURDATE(), i.END_TIME)";
            case "pendingDays" -> "DATEDIFF(CURDATE(), i.UPDATED_AT)";
            case "createdAt" -> "i.CREATED_AT";
            case "updatedAt" -> "i.UPDATED_AT";
            case "boardName" -> "b.BOARD_NAME";
            default -> "i.END_TIME";
        };

        // NULL 처리 (NULLS LAST)
        if ("endTime".equals(safeSortField) || "dueDate".equals(safeSortField) || "overdueDays".equals(safeSortField)) {
            return field + " IS NULL, " + field + " " + safeDirection;
        }

        return field + " " + safeDirection;
    }
}
