package com.taskflow.controller;

import com.taskflow.common.ApiResponse;
import com.taskflow.dto.todo.TodoShareRequest;
import com.taskflow.dto.todo.TodoShareResponse;
import com.taskflow.dto.todo.TodoShareUpdateRequest;
import com.taskflow.security.SecurityUtils;
import com.taskflow.service.TodoShareService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Todo 공유 컨트롤러
 *
 * API:
 * - GET /api/todos/{todoId}/shares - Todo 공유 목록 조회
 * - POST /api/todos/{todoId}/shares - Todo 공유 추가
 * - PUT /api/todos/shares/{shareId} - 공유 권한 변경
 * - DELETE /api/todos/shares/{shareId} - 공유 삭제
 * - DELETE /api/todos/{todoId}/shares/{userId} - 특정 사용자 공유 삭제
 */
@Slf4j
@RestController
@RequestMapping("/api/todos")
@RequiredArgsConstructor
public class TodoShareController {

    private final TodoShareService todoShareService;

    /**
     * Todo 공유 목록 조회
     */
    @GetMapping("/{todoId}/shares")
    public ResponseEntity<ApiResponse<List<TodoShareResponse>>> getShares(
            @PathVariable("todoId") Long todoId
    ) {
        log.debug("Get todo shares: todoId={}", todoId);

        List<TodoShareResponse> response = todoShareService.getShares(todoId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Todo 공유 추가
     */
    @PostMapping("/{todoId}/shares")
    public ResponseEntity<ApiResponse<TodoShareResponse>> addShare(
            @PathVariable("todoId") Long todoId,
            @Valid @RequestBody TodoShareRequest request
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        String currentUsername = SecurityUtils.getCurrentUsername();
        log.info("Add todo share: todoId={}, sharedUsername={}", todoId, request.getSharedUsername());

        TodoShareResponse response = todoShareService.addShare(todoId, request, currentUserId, currentUsername);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * 공유 권한 변경
     */
    @PutMapping("/shares/{shareId}")
    public ResponseEntity<ApiResponse<TodoShareResponse>> updatePermission(
            @PathVariable("shareId") Long shareId,
            @Valid @RequestBody TodoShareUpdateRequest request
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Update todo share permission: shareId={}, permission={}", shareId, request.getPermission());

        TodoShareResponse response = todoShareService.updatePermission(shareId, request, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 공유 삭제
     */
    @DeleteMapping("/shares/{shareId}")
    public ResponseEntity<ApiResponse<Void>> deleteShare(
            @PathVariable("shareId") Long shareId
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Delete todo share: shareId={}", shareId);

        todoShareService.deleteShare(shareId, currentUserId);
        return ResponseEntity.ok(ApiResponse.successWithMessage("공유가 해제되었습니다"));
    }

    /**
     * 특정 사용자에 대한 공유 삭제
     */
    @DeleteMapping("/{todoId}/shares/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteShareByUser(
            @PathVariable("todoId") Long todoId,
            @PathVariable("userId") Long userId
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        log.info("Delete todo share by user: todoId={}, userId={}", todoId, userId);

        todoShareService.deleteShareByUser(todoId, userId, currentUserId);
        return ResponseEntity.ok(ApiResponse.successWithMessage("공유가 해제되었습니다"));
    }
}
