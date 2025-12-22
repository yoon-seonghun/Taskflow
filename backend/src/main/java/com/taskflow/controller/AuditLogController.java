package com.taskflow.controller;

import com.taskflow.common.ApiResponse;
import com.taskflow.dto.audit.AuditLogPageResponse;
import com.taskflow.dto.audit.AuditLogResponse;
import com.taskflow.dto.audit.AuditLogSearchRequest;
import com.taskflow.security.SecurityUtils;
import com.taskflow.service.AuditLogService;
import com.taskflow.service.BoardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 감사 로그 컨트롤러
 *
 * API:
 * - GET /api/audit-logs - 감사 로그 검색
 * - GET /api/audit-logs/recent - 최근 이력 조회
 * - GET /api/audit-logs/boards/{boardId} - 보드별 이력 조회
 * - GET /api/audit-logs/items/{itemId} - 업무별 이력 조회
 */
@Slf4j
@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;
    private final BoardService boardService;

    /**
     * 감사 로그 검색
     */
    @GetMapping
    public ResponseEntity<ApiResponse<AuditLogPageResponse>> search(
            @RequestParam(value = "targetType", required = false) String targetType,
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "actorId", required = false) Long actorId,
            @RequestParam(value = "startDate", required = false) LocalDate startDate,
            @RequestParam(value = "endDate", required = false) LocalDate endDate,
            @RequestParam(value = "page", required = false, defaultValue = "0") Integer page,
            @RequestParam(value = "size", required = false, defaultValue = "20") Integer size
    ) {
        log.debug("Search audit logs: targetType={}, action={}", targetType, action);

        AuditLogSearchRequest request = new AuditLogSearchRequest();
        request.setTargetType(targetType);
        request.setAction(action);
        request.setActorId(actorId);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setPage(page);
        request.setSize(size);

        AuditLogPageResponse response = auditLogService.search(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 최근 이력 조회
     */
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getRecent(
            @RequestParam(value = "limit", required = false, defaultValue = "20") Integer limit
    ) {
        log.debug("Get recent audit logs: limit={}", limit);

        List<AuditLogResponse> response = auditLogService.getRecent(limit);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 보드별 이력 조회
     */
    @GetMapping("/boards/{boardId}")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getByBoard(
            @PathVariable("boardId") Long boardId
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        log.debug("Get audit logs for board: boardId={}", boardId);

        // 접근 권한 확인
        if (!boardService.hasAccess(boardId, currentUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("보드에 접근 권한이 없습니다"));
        }

        List<AuditLogResponse> response = auditLogService.getByBoard(boardId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 업무별 이력 조회
     */
    @GetMapping("/items/{itemId}")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> getByItem(
            @PathVariable("itemId") Long itemId
    ) {
        log.debug("Get audit logs for item: itemId={}", itemId);

        List<AuditLogResponse> response = auditLogService.getByItem(itemId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
