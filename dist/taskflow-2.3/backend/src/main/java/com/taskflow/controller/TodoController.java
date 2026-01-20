package com.taskflow.controller;

import com.taskflow.common.ApiResponse;
import com.taskflow.dto.todo.*;
import com.taskflow.security.SecurityUtils;
import com.taskflow.service.TodoService;
import com.taskflow.service.TodoShareService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 개인 Todo 컨트롤러
 *
 * API:
 * - GET /api/todos - 내 Todo 목록 조회
 * - GET /api/todos/{todoId} - Todo 상세 조회
 * - POST /api/todos - Todo 생성
 * - PUT /api/todos/{todoId} - Todo 수정
 * - PUT /api/todos/{todoId}/complete - 완료 상태 토글
 * - DELETE /api/todos/{todoId} - Todo 삭제
 * - PUT /api/todos/{todoId}/transfer - Todo 이관
 * - PUT /api/todos/reorder - Todo 순서 변경
 * - GET /api/todos/today - 오늘 마감 Todo 목록
 * - GET /api/todos/overdue - 지연 Todo 목록
 * - GET /api/todos/completed - 완료된 Todo 목록
 * - GET /api/todos/transferred - 이관받은 Todo 목록
 * - GET /api/todos/shared - 공유받은 Todo 목록
 * - GET /api/todos/count - Todo 수 조회
 * - PUT /api/todos/transfer/bulk - 일괄 이관 (사용자 삭제용)
 */
@Slf4j
@RestController
@RequestMapping("/api/todos")
@RequiredArgsConstructor
public class TodoController {

    private final TodoService todoService;
    private final TodoShareService todoShareService;

    /**
     * 내 Todo 목록 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<TodoResponse>>> getTodos(
            @RequestParam(value = "includeCompleted", required = false, defaultValue = "false") Boolean includeCompleted,
            @RequestParam(value = "dueDateFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDateFrom,
            @RequestParam(value = "dueDateTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueDateTo,
            @RequestParam(value = "groupId", required = false) Long groupId
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        log.debug("Get todos: userId={}, groupId={}", currentUserId, groupId);

        List<TodoResponse> response = todoService.getTodos(currentUserId, includeCompleted, dueDateFrom, dueDateTo, groupId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Todo 상세 조회
     */
    @GetMapping("/{todoId}")
    public ResponseEntity<ApiResponse<TodoResponse>> getTodo(
            @PathVariable("todoId") Long todoId
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        log.debug("Get todo: todoId={}", todoId);

        TodoResponse response = todoService.getTodo(todoId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Todo 생성
     */
    @PostMapping
    public ResponseEntity<ApiResponse<TodoResponse>> createTodo(
            @Valid @RequestBody TodoCreateRequest request
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        String currentUsername = SecurityUtils.getCurrentUsername();
        log.info("Create todo: userId={}", currentUserId);

        TodoResponse response = todoService.createTodo(currentUserId, request, currentUsername);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /**
     * Todo 수정
     */
    @PutMapping("/{todoId}")
    public ResponseEntity<ApiResponse<TodoResponse>> updateTodo(
            @PathVariable("todoId") Long todoId,
            @Valid @RequestBody TodoUpdateRequest request
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        String currentUsername = SecurityUtils.getCurrentUsername();
        log.info("Update todo: todoId={}", todoId);

        TodoResponse response = todoService.updateTodo(todoId, request, currentUserId, currentUsername);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 완료 상태 토글
     */
    @PutMapping("/{todoId}/complete")
    public ResponseEntity<ApiResponse<TodoResponse>> toggleComplete(
            @PathVariable("todoId") Long todoId
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        String currentUsername = SecurityUtils.getCurrentUsername();
        log.info("Toggle todo complete: todoId={}", todoId);

        TodoResponse response = todoService.toggleComplete(todoId, currentUserId, currentUsername);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Todo 삭제
     */
    @DeleteMapping("/{todoId}")
    public ResponseEntity<ApiResponse<Void>> deleteTodo(
            @PathVariable("todoId") Long todoId
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        String currentUsername = SecurityUtils.getCurrentUsername();
        log.info("Delete todo: todoId={}", todoId);

        todoService.deleteTodo(todoId, currentUserId, currentUsername);
        return ResponseEntity.ok(ApiResponse.successWithMessage("Todo가 삭제되었습니다"));
    }

    /**
     * Todo 이관
     */
    @PutMapping("/{todoId}/transfer")
    public ResponseEntity<ApiResponse<Void>> transferTodo(
            @PathVariable("todoId") Long todoId,
            @Valid @RequestBody TodoTransferRequest request
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        String currentUsername = SecurityUtils.getCurrentUsername();
        log.info("Transfer todo: todoId={}, toUsername={}", todoId, request.getToUsername());

        todoService.transferTodo(todoId, request, currentUserId, currentUsername);
        return ResponseEntity.ok(ApiResponse.successWithMessage("Todo가 이관되었습니다"));
    }

    /**
     * Todo 순서 변경
     */
    @PutMapping("/reorder")
    public ResponseEntity<ApiResponse<Void>> reorderTodos(
            @Valid @RequestBody TodoReorderRequest request
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        String currentUsername = SecurityUtils.getCurrentUsername();
        log.info("Reorder todos: count={}", request.getTodoIds().size());

        todoService.reorderTodos(currentUserId, request, currentUsername);
        return ResponseEntity.ok(ApiResponse.successWithMessage("순서가 변경되었습니다"));
    }

    /**
     * 오늘 마감 Todo 목록
     */
    @GetMapping("/today")
    public ResponseEntity<ApiResponse<List<TodoResponse>>> getTodayTodos() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        log.debug("Get today todos: userId={}", currentUserId);

        List<TodoResponse> response = todoService.getTodayTodos(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 지연 Todo 목록
     */
    @GetMapping("/overdue")
    public ResponseEntity<ApiResponse<List<TodoResponse>>> getOverdueTodos() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        log.debug("Get overdue todos: userId={}", currentUserId);

        List<TodoResponse> response = todoService.getOverdueTodos(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 완료된 Todo 목록
     */
    @GetMapping("/completed")
    public ResponseEntity<ApiResponse<List<TodoResponse>>> getCompletedTodos(
            @RequestParam(value = "completedDateFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate completedDateFrom,
            @RequestParam(value = "completedDateTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate completedDateTo
    ) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        log.debug("Get completed todos: userId={}", currentUserId);

        List<TodoResponse> response = todoService.getCompletedTodos(currentUserId, completedDateFrom, completedDateTo);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 이관받은 Todo 목록
     */
    @GetMapping("/transferred")
    public ResponseEntity<ApiResponse<List<TodoResponse>>> getTransferredTodos() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        log.debug("Get transferred todos: userId={}", currentUserId);

        List<TodoResponse> response = todoService.getTransferredTodos(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 공유받은 Todo 목록
     */
    @GetMapping("/shared")
    public ResponseEntity<ApiResponse<List<TodoShareResponse>>> getSharedTodos() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        log.debug("Get shared todos: userId={}", currentUserId);

        List<TodoShareResponse> response = todoService.getSharedTodos(currentUserId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Todo 수 조회 (사용자별)
     */
    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> getTodoCount(
            @RequestParam(value = "userId", required = false) Long userId
    ) {
        Long targetUserId = userId != null ? userId : SecurityUtils.getCurrentUserId();
        log.debug("Get todo count: userId={}", targetUserId);

        int totalCount = todoService.getTodoCount(targetUserId);
        int activeCount = todoService.getActiveTodoCount(targetUserId);

        Map<String, Integer> response = Map.of(
                "total", totalCount,
                "active", activeCount
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 일괄 이관 (사용자 삭제용)
     */
    @PutMapping("/transfer/bulk")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> transferAllTodos(
            @Valid @RequestBody TodoBulkTransferRequest request
    ) {
        String currentUsername = SecurityUtils.getCurrentUsername();
        log.info("Bulk transfer todos: fromUserId={}, toUserId={}",
                request.getFromUserId(), request.getToUserId());

        int count = todoService.transferAllTodos(request, currentUsername);

        Map<String, Integer> response = Map.of("transferredCount", count);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 사용자의 모든 Todo 삭제 (사용자 삭제용)
     */
    @DeleteMapping("/by-user/{userId}")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> deleteAllTodos(
            @PathVariable("userId") Long userId
    ) {
        String currentUsername = SecurityUtils.getCurrentUsername();
        log.info("Delete all todos for user: userId={}", userId);

        int count = todoService.deleteAllTodos(userId, currentUsername);

        Map<String, Integer> response = Map.of("deletedCount", count);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
