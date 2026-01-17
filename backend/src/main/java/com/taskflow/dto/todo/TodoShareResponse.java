package com.taskflow.dto.todo;

import com.taskflow.domain.TodoShare;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Todo 공유 응답 DTO
 */
@Getter
@Builder
public class TodoShareResponse {

    private Long shareId;
    private Long todoId;
    private String todoTitle;
    private Long sharedUserId;
    private String sharedUserName;
    private String sharedUsername;
    private String departmentName;
    private Long sharedByUserId;
    private String sharedByUserName;
    private String sharedByUsername;
    private String permission;
    private LocalDateTime createdAt;
    private String createdBy;

    // Todo 관련 필드
    private LocalDate todoDueDate;
    private LocalTime todoDueTime;
    private String todoPriority;
    private String todoOwnerUserName;

    /**
     * Domain -> Response 변환
     */
    public static TodoShareResponse from(TodoShare share) {
        if (share == null) return null;

        return TodoShareResponse.builder()
                .shareId(share.getShareId())
                .todoId(share.getTodoId())
                .todoTitle(share.getTodoTitle())
                .sharedUserId(share.getSharedUserId())
                .sharedUserName(share.getSharedUserName())
                .sharedUsername(share.getSharedUsername())
                .departmentName(share.getDepartmentName())
                .sharedByUserId(share.getSharedByUserId())
                .sharedByUserName(share.getSharedByUserName())
                .sharedByUsername(share.getSharedByUsername())
                .permission(share.getPermission())
                .createdAt(share.getCreatedAt())
                .createdBy(share.getCreatedBy())
                .todoDueDate(share.getTodoDueDate())
                .todoDueTime(share.getTodoDueTime())
                .todoPriority(share.getTodoPriority())
                .todoOwnerUserName(share.getTodoOwnerUserName())
                .build();
    }
}
