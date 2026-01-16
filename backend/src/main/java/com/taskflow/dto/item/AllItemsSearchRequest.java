package com.taskflow.dto.item;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Set;

/**
 * 전체 업무 조회 요청 DTO (v2.1)
 * - 소유 보드 + 공유 보드 + 개별 공유/배당 업무 통합 조회
 * - 하위 업무 필터 지원
 */
@Getter
@Setter
public class AllItemsSearchRequest {

    /**
     * 허용된 정렬 필드 목록 (SQL 인젝션 방지)
     */
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "title", "content", "status", "priority", "assigneeUsername",
            "groupId", "categoryId", "requestDate", "dueDate",
            "createdAt", "updatedAt", "boardName", "sourceType"
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
     * 출처 타입 필터 (OWNED, BOARD_SHARED, ITEM_SHARED, ASSIGNED, ALL)
     */
    private String sourceType = "ALL";

    /**
     * 보드 ID 필터 (특정 보드만 조회 시)
     */
    private Long boardId;

    /**
     * 그룹 ID 필터
     */
    private Long groupId;

    /**
     * 담당자 USERNAME 필터
     */
    private String assigneeUsername;

    /**
     * 배당자 USERNAME 필터 (v2.1)
     */
    private String assignedByUsername;

    /**
     * 시작일 (생성일 기준)
     */
    private LocalDate startDate;

    /**
     * 종료일 (생성일 기준)
     */
    private LocalDate endDate;

    /**
     * 완료 업무 포함 여부
     */
    private Boolean includeCompleted = false;

    /**
     * 삭제 업무 포함 여부
     */
    private Boolean includeDeleted = false;

    /**
     * 하위 업무 포함 여부 (v2.2)
     */
    private Boolean includeChildren = true;

    /**
     * 특정 부모 업무의 하위 업무만 조회 (v2.2)
     */
    private Long parentItemId;

    /**
     * 특정 깊이 필터 (0/1/2) (v2.2)
     */
    private Integer depthFilter;

    /**
     * 페이지 번호 (0부터 시작)
     */
    private Integer page = 0;

    /**
     * 페이지 크기
     */
    private Integer size = 50;

    /**
     * 정렬 필드
     */
    private String sortField = "createdAt";

    /**
     * 정렬 방향 (asc, desc)
     */
    private String sortDirection = "desc";

    /**
     * 오프셋 계산 (NPE 방지)
     */
    public int getOffset() {
        int safePage = (page != null) ? page : 0;
        int safeSize = (size != null) ? size : 50;
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
        return (size != null) ? size : 50;
    }

    /**
     * 정렬 SQL 문자열 반환 (SQL 인젝션 방지)
     */
    public String getOrderBy() {
        // 정렬 필드 화이트리스트 검증
        String safeSortField = (sortField != null && ALLOWED_SORT_FIELDS.contains(sortField))
                ? sortField
                : "createdAt";

        // 정렬 방향 엄격한 검증
        String safeDirection;
        if ("desc".equalsIgnoreCase(sortDirection)) {
            safeDirection = "DESC";
        } else if ("asc".equalsIgnoreCase(sortDirection)) {
            safeDirection = "ASC";
        } else {
            safeDirection = "DESC";
        }

        // 서브쿼리 외부에서 ORDER BY하므로 테이블 별칭 없이 컬럼명만 사용
        String field = switch (safeSortField) {
            case "title", "content" -> "TITLE";
            case "status" -> "STATUS";
            case "priority" -> "FIELD(PRIORITY, 'URGENT', 'HIGH', 'NORMAL', 'LOW')";
            case "assigneeUsername" -> "ASSIGNEE_USERNAME";
            case "groupId" -> "GROUP_ID";
            case "categoryId" -> "CATEGORY_ID";
            case "requestDate" -> "REQUEST_DATE";
            case "dueDate" -> "DUE_DATE";
            case "createdAt" -> "CREATED_AT";
            case "updatedAt" -> "UPDATED_AT";
            case "boardName" -> "BOARD_NAME";
            case "sourceType" -> "SOURCE_TYPE";
            default -> "CREATED_AT";
        };

        // NULL 처리 (NULLS LAST)
        if ("dueDate".equals(safeSortField) || "requestDate".equals(safeSortField)) {
            return field + " IS NULL, " + field + " " + safeDirection;
        }

        return field + " " + safeDirection;
    }
}
