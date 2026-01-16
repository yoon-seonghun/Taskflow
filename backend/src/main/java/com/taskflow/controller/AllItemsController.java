package com.taskflow.controller;

import com.taskflow.common.ApiResponse;
import com.taskflow.dto.item.AllItemsPageResponse;
import com.taskflow.dto.item.AllItemsSearchRequest;
import com.taskflow.dto.item.AllItemsStats;
import com.taskflow.security.SecurityUtils;
import com.taskflow.service.AllItemsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 전체 업무 컨트롤러 (v2.1)
 *
 * API:
 * - GET /api/items/all - 전체 업무 목록 (소유+공유+배당)
 * - GET /api/items/all/stats - 전체 업무 통계
 */
@Slf4j
@RestController
@RequestMapping("/api/items/all")
@RequiredArgsConstructor
public class AllItemsController {

    private final AllItemsService allItemsService;

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
     * 전체 업무 목록 조회
     * - 소유 보드 업무 + 공유 보드 업무 + 개별 공유 업무 + 배당받은 업무
     */
    @GetMapping
    public ResponseEntity<ApiResponse<AllItemsPageResponse>> getAllItems(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "priority", required = false) String priority,
            @RequestParam(value = "sourceType", required = false, defaultValue = "ALL") String sourceType,
            @RequestParam(value = "boardId", required = false) Long boardId,
            @RequestParam(value = "groupId", required = false) Long groupId,
            @RequestParam(value = "assigneeUsername", required = false) String assigneeUsername,
            @RequestParam(value = "assignedByUsername", required = false) String assignedByUsername,
            @RequestParam(value = "startDate", required = false) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) LocalDate endDate,
            @RequestParam(value = "includeCompleted", required = false, defaultValue = "false") Boolean includeCompleted,
            @RequestParam(value = "includeDeleted", required = false, defaultValue = "false") Boolean includeDeleted,
            @RequestParam(value = "includeChildren", required = false, defaultValue = "true") Boolean includeChildren,
            @RequestParam(value = "parentItemId", required = false) Long parentItemId,
            @RequestParam(value = "depthFilter", required = false) Integer depthFilter,
            @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "50") Integer size,
            @RequestParam(value = "sort", required = false, defaultValue = "createdAt,desc") String sort
    ) {
        String currentUsername = SecurityUtils.getCurrentUsername();

        // 정렬 파라미터 파싱
        String[] sortParams = parseSortParams(sort, "createdAt", "desc");

        // 검색 조건 생성
        AllItemsSearchRequest request = new AllItemsSearchRequest();
        request.setKeyword(keyword);
        request.setStatus(status);
        request.setPriority(priority);
        request.setSourceType(sourceType);
        request.setBoardId(boardId);
        request.setGroupId(groupId);
        request.setAssigneeUsername(assigneeUsername);
        request.setAssignedByUsername(assignedByUsername);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setIncludeCompleted(includeCompleted);
        request.setIncludeDeleted(includeDeleted);
        request.setIncludeChildren(includeChildren);
        request.setParentItemId(parentItemId);
        request.setDepthFilter(depthFilter);
        request.setPage(page);
        request.setSize(size);
        request.setSortField(sortParams[0]);
        request.setSortDirection(sortParams[1]);

        log.debug("Get all items: username={}, sourceType={}, page={}, size={}",
                currentUsername, sourceType, page, size);

        AllItemsPageResponse response = allItemsService.getAllItems(currentUsername, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 전체 업무 통계 조회
     * - 출처별, 상태별, 우선순위별 개수
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<AllItemsStats>> getStats(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "priority", required = false) String priority,
            @RequestParam(value = "boardId", required = false) Long boardId,
            @RequestParam(value = "includeCompleted", required = false, defaultValue = "false") Boolean includeCompleted,
            @RequestParam(value = "includeDeleted", required = false, defaultValue = "false") Boolean includeDeleted,
            @RequestParam(value = "includeChildren", required = false, defaultValue = "true") Boolean includeChildren
    ) {
        String currentUsername = SecurityUtils.getCurrentUsername();

        AllItemsSearchRequest request = new AllItemsSearchRequest();
        request.setStatus(status);
        request.setPriority(priority);
        request.setBoardId(boardId);
        request.setIncludeCompleted(includeCompleted);
        request.setIncludeDeleted(includeDeleted);
        request.setIncludeChildren(includeChildren);

        log.debug("Get all items stats: username={}", currentUsername);

        AllItemsStats stats = allItemsService.getStats(currentUsername, request);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
