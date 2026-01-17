package com.taskflow.dto.todo;

import com.taskflow.domain.Todo;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Todo 응답 DTO
 */
@Getter
@Builder
public class TodoResponse {

    private Long todoId;
    private Long ownerUserId;
    private String ownerUserName;
    private String ownerUsername;
    private String title;
    private String description;
    private String priority;
    private LocalDate dueDate;
    private LocalTime dueTime;
    private Boolean isCompleted;
    private LocalDateTime completedAt;
    private Integer sortOrder;
    private String repeatType;
    private Integer repeatInterval;
    private String repeatDays;
    private LocalDate repeatEndDate;
    private LocalDate nextDueDate;
    private Long parentTodoId;
    private Long transferredFromUserId;
    private String transferredFromUserName;
    private LocalDateTime transferredAt;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;

    // 추가 정보
    private Integer shareCount;
    private Boolean isOverdue;
    private Boolean isDueToday;
    private List<TodoShareResponse> shares;

    /**
     * Domain -> Response 변환
     */
    public static TodoResponse from(Todo todo) {
        if (todo == null) return null;

        return TodoResponse.builder()
                .todoId(todo.getTodoId())
                .ownerUserId(todo.getOwnerUserId())
                .ownerUserName(todo.getOwnerUserName())
                .ownerUsername(todo.getOwnerUsername())
                .title(todo.getTitle())
                .description(todo.getDescription())
                .priority(todo.getPriority())
                .dueDate(todo.getDueDate())
                .dueTime(todo.getDueTime())
                .isCompleted(todo.getIsCompleted())
                .completedAt(todo.getCompletedAt())
                .sortOrder(todo.getSortOrder())
                .repeatType(todo.getRepeatType())
                .repeatInterval(todo.getRepeatInterval())
                .repeatDays(todo.getRepeatDays())
                .repeatEndDate(todo.getRepeatEndDate())
                .nextDueDate(todo.getNextDueDate())
                .parentTodoId(todo.getParentTodoId())
                .transferredFromUserId(todo.getTransferredFromUserId())
                .transferredFromUserName(todo.getTransferredFromUserName())
                .transferredAt(todo.getTransferredAt())
                .createdAt(todo.getCreatedAt())
                .createdBy(todo.getCreatedBy())
                .updatedAt(todo.getUpdatedAt())
                .updatedBy(todo.getUpdatedBy())
                .shareCount(todo.getShareCount())
                .isOverdue(todo.isOverdue())
                .isDueToday(todo.isDueToday())
                .build();
    }

    /**
     * Domain -> Response 변환 (공유 목록 포함)
     */
    public static TodoResponse from(Todo todo, List<TodoShareResponse> shares) {
        if (todo == null) return null;

        return TodoResponse.builder()
                .todoId(todo.getTodoId())
                .ownerUserId(todo.getOwnerUserId())
                .ownerUserName(todo.getOwnerUserName())
                .ownerUsername(todo.getOwnerUsername())
                .title(todo.getTitle())
                .description(todo.getDescription())
                .priority(todo.getPriority())
                .dueDate(todo.getDueDate())
                .dueTime(todo.getDueTime())
                .isCompleted(todo.getIsCompleted())
                .completedAt(todo.getCompletedAt())
                .sortOrder(todo.getSortOrder())
                .repeatType(todo.getRepeatType())
                .repeatInterval(todo.getRepeatInterval())
                .repeatDays(todo.getRepeatDays())
                .repeatEndDate(todo.getRepeatEndDate())
                .nextDueDate(todo.getNextDueDate())
                .parentTodoId(todo.getParentTodoId())
                .transferredFromUserId(todo.getTransferredFromUserId())
                .transferredFromUserName(todo.getTransferredFromUserName())
                .transferredAt(todo.getTransferredAt())
                .createdAt(todo.getCreatedAt())
                .createdBy(todo.getCreatedBy())
                .updatedAt(todo.getUpdatedAt())
                .updatedBy(todo.getUpdatedBy())
                .shareCount(shares != null ? shares.size() : todo.getShareCount())
                .isOverdue(todo.isOverdue())
                .isDueToday(todo.isDueToday())
                .shares(shares)
                .build();
    }
}
