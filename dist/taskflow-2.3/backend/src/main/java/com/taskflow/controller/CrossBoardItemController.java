package com.taskflow.controller;

import com.taskflow.common.ApiResponse;
import com.taskflow.dto.item.*;
import com.taskflow.security.SecurityUtils;
import com.taskflow.service.ItemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/**
 * Cross-board 아이템 컨트롤러
 *
 * 여러 보드에 걸친 아이템 조회 API
 *
 * API:
 * - GET /api/items/overdue - 지연 업무 목록 (전체 보드)
 * - GET /api/items/pending - 보류 업무 목록 (전체 보드)
 * - GET /api/items/active - 활성 업무 목록 (전체 보드)
 * - GET /api/items/shared - 공유받은 업무 목록
 * - GET /api/items/stats - 업무 통계
 */
@Slf4j
@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class CrossBoardItemController {

    private final ItemService itemService;

    /**
     * 정렬 파라미터 파싱 (안전한 처리)
     */
    private String[] parseSortParams(String sort, String defaultField, String defaultDirection) {
        if (sort == null || sort.isEmpty()) {
            return new String[]{defaultField, defaultDirection};
        }
        String[] parts = sort.split(",");
        String field = parts.length > 0 && !parts[0].isEmpty() ? parts[0] : defaultField;
        String direction = parts.length > 1 && !parts[1].isEmpty() ? parts[1] : defaultDirection;
        return new String[]{field, direction};
    }

    /**
     * 지연 업무 목록 조회 (Cross-board)
     * - 마감일이 지난 업무
     * - 사용자가 접근 가능한 모든 보드 대상
     */
    @GetMapping("/overdue")
    public ResponseEntity<ApiResponse<ItemPageResponse>> getOverdueItems(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "priority", required = false) String priority,
            @RequestParam(value = "assigneeId", required = false) Long assigneeId,
            @RequestParam(value = "groupId", required = false) Long groupId,
            @RequestParam(value = "boardId", required = false) Long boardId,
            @RequestParam(value = "startDate", required = false) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) LocalDate endDate,
            @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "20") Integer size,
            @RequestParam(value = "sort", required = false, defaultValue = "dueDate,asc") String sort
    ) {
        String currentUsername = SecurityUtils.getCurrentUsername();

        // 정렬 파라미터 파싱 (안전한 처리)
        String[] sortParams = parseSortParams(sort, "dueDate", "asc");

        // 검색 조건 생성
        CrossBoardSearchRequest request = new CrossBoardSearchRequest();
        request.setKeyword(keyword);
        request.setPriority(priority);
        request.setAssigneeId(assigneeId);
        request.setGroupId(groupId);
        request.setBoardId(boardId);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setPage(page);
        request.setSize(size);
        request.setSortField(sortParams[0]);
        request.setSortDirection(sortParams[1]);
        request.setOverdueOnly(true);

        log.debug("Get overdue items: username={}, request={}", currentUsername, request);

        ItemPageResponse response = itemService.getOverdueItems(currentUsername, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 보류 업무 목록 조회 (Cross-board)
     * - 상태가 PENDING인 업무
     * - 사용자가 접근 가능한 모든 보드 대상
     */
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<ItemPageResponse>> getPendingItems(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "priority", required = false) String priority,
            @RequestParam(value = "assigneeId", required = false) Long assigneeId,
            @RequestParam(value = "groupId", required = false) Long groupId,
            @RequestParam(value = "boardId", required = false) Long boardId,
            @RequestParam(value = "startDate", required = false) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) LocalDate endDate,
            @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "20") Integer size,
            @RequestParam(value = "sort", required = false, defaultValue = "updatedAt,desc") String sort
    ) {
        String currentUsername = SecurityUtils.getCurrentUsername();

        // 정렬 파라미터 파싱 (안전한 처리)
        String[] sortParams = parseSortParams(sort, "updatedAt", "desc");

        // 검색 조건 생성
        CrossBoardSearchRequest request = new CrossBoardSearchRequest();
        request.setKeyword(keyword);
        request.setPriority(priority);
        request.setAssigneeId(assigneeId);
        request.setGroupId(groupId);
        request.setBoardId(boardId);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setPage(page);
        request.setSize(size);
        request.setSortField(sortParams[0]);
        request.setSortDirection(sortParams[1]);
        request.setStatus("PENDING");

        log.debug("Get pending items: username={}, request={}", currentUsername, request);

        ItemPageResponse response = itemService.getPendingItems(currentUsername, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 활성 업무 목록 조회 (Cross-board)
     * - 완료/삭제 제외 업무
     * - 사용자가 접근 가능한 모든 보드 대상
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<ItemPageResponse>> getActiveItems(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "priority", required = false) String priority,
            @RequestParam(value = "assigneeId", required = false) Long assigneeId,
            @RequestParam(value = "groupId", required = false) Long groupId,
            @RequestParam(value = "boardId", required = false) Long boardId,
            @RequestParam(value = "overdueOnly", required = false, defaultValue = "false") Boolean overdueOnly,
            @RequestParam(value = "startDate", required = false) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) LocalDate endDate,
            @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "20") Integer size,
            @RequestParam(value = "sort", required = false, defaultValue = "createdAt,desc") String sort
    ) {
        String currentUsername = SecurityUtils.getCurrentUsername();

        // 정렬 파라미터 파싱 (안전한 처리)
        String[] sortParams = parseSortParams(sort, "createdAt", "desc");

        // 검색 조건 생성
        CrossBoardSearchRequest request = new CrossBoardSearchRequest();
        request.setKeyword(keyword);
        request.setStatus(status);
        request.setPriority(priority);
        request.setAssigneeId(assigneeId);
        request.setGroupId(groupId);
        request.setBoardId(boardId);
        request.setOverdueOnly(overdueOnly);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setPage(page);
        request.setSize(size);
        request.setSortField(sortParams[0]);
        request.setSortDirection(sortParams[1]);

        log.debug("Get active items cross-board: username={}, request={}", currentUsername, request);

        ItemPageResponse response = itemService.getActiveItemsCrossBoard(currentUsername, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 공유받은 업무 목록 조회
     * - 다른 사용자가 공유한 업무
     * - TB_ITEM_SHARE 테이블 기반
     */
    @GetMapping("/shared")
    public ResponseEntity<ApiResponse<ItemPageResponse>> getSharedItems(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "priority", required = false) String priority,
            @RequestParam(value = "includeCompleted", required = false, defaultValue = "false") Boolean includeCompleted,
            @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "20") Integer size,
            @RequestParam(value = "sort", required = false, defaultValue = "createdAt,desc") String sort
    ) {
        String currentUsername = SecurityUtils.getCurrentUsername();

        // 정렬 파라미터 파싱 (안전한 처리)
        String[] sortParams = parseSortParams(sort, "createdAt", "desc");

        // 검색 조건 생성
        CrossBoardSearchRequest request = new CrossBoardSearchRequest();
        request.setKeyword(keyword);
        request.setStatus(status);
        request.setPriority(priority);
        request.setPage(page);
        request.setSize(size);
        request.setSortField(sortParams[0]);
        request.setSortDirection(sortParams[1]);

        log.debug("Get shared items: username={}, request={}", currentUsername, request);

        ItemPageResponse response = itemService.getSharedItems(currentUsername, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 업무 통계 조회 (Cross-board)
     * - 지연/보류/활성 업무 개수
     * - 우선순위별 통계
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        String currentUsername = SecurityUtils.getCurrentUsername();

        log.debug("Get cross-board stats: username={}", currentUsername);

        Map<String, Object> stats = itemService.getCrossBoardStats(currentUsername);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
