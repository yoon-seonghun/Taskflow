package com.taskflow.controller;

import com.taskflow.common.ApiResponse;
import com.taskflow.dto.history.*;
import com.taskflow.service.HistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 이력 컨트롤러
 *
 * API:
 * - GET /api/history/items - 작업 처리 이력
 * - GET /api/history/templates - 작업 등록 이력
 */
@Slf4j
@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class HistoryController {

    private final HistoryService historyService;

    /**
     * 작업 처리 이력 조회 (완료/삭제된 아이템)
     */
    @GetMapping("/items")
    public ResponseEntity<ApiResponse<ItemHistoryPageResponse>> getItemHistory(
            @RequestParam(value = "boardId", required = false) Long boardId,
            @RequestParam(value = "result", required = false) String result,
            @RequestParam(value = "workerId", required = false) Long workerId,
            @RequestParam(value = "startDate", required = false) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) LocalDate endDate,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "20") Integer size,
            @RequestParam(value = "sort", required = false, defaultValue = "completedAt,desc") String sort
    ) {
        log.debug("Get item history: boardId={}, result={}, workerId={}", boardId, result, workerId);

        // 정렬 파라미터 파싱
        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        String sortDirection = sortParts.length > 1 ? sortParts[1] : "desc";

        // 검색 조건 생성
        ItemHistorySearchRequest request = new ItemHistorySearchRequest();
        request.setBoardId(boardId);
        request.setResult(result);
        request.setWorkerId(workerId);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setKeyword(keyword);
        request.setPage(page);
        request.setSize(size);
        request.setSortField(sortField);
        request.setSortDirection(sortDirection);

        ItemHistoryPageResponse response = historyService.getItemHistory(request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 작업 등록 이력 조회 (템플릿)
     */
    @GetMapping("/templates")
    public ResponseEntity<ApiResponse<TemplateHistoryPageResponse>> getTemplateHistory(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "createdBy", required = false) Long createdBy,
            @RequestParam(value = "startDate", required = false) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) LocalDate endDate,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "20") Integer size,
            @RequestParam(value = "sort", required = false, defaultValue = "createdAt,desc") String sort
    ) {
        log.debug("Get template history: status={}, createdBy={}", status, createdBy);

        // 정렬 파라미터 파싱
        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        String sortDirection = sortParts.length > 1 ? sortParts[1] : "desc";

        // 검색 조건 생성
        TemplateHistorySearchRequest request = new TemplateHistorySearchRequest();
        request.setStatus(status);
        request.setCreatedBy(createdBy);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setKeyword(keyword);
        request.setPage(page);
        request.setSize(size);
        request.setSortField(sortField);
        request.setSortDirection(sortDirection);

        TemplateHistoryPageResponse response = historyService.getTemplateHistory(request);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
