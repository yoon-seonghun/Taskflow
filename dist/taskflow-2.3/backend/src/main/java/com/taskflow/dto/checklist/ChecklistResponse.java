package com.taskflow.dto.checklist;

import com.taskflow.domain.ItemChecklist;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 체크리스트 응답 DTO
 */
@Getter
@Builder
public class ChecklistResponse {

    private Long checklistId;
    private Long itemId;
    private String title;
    private Boolean isCompleted;
    private LocalDateTime completedAt;
    private String completedBy;
    private String completedByName;
    private Integer sortOrder;
    private LocalDate dueDate;
    private String assigneeUsername;
    private String assigneeName;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;

    // 추가 정보
    private Boolean isOverdue;
    private Boolean isDueToday;

    /**
     * Domain -> Response 변환
     */
    public static ChecklistResponse from(ItemChecklist checklist) {
        if (checklist == null) return null;

        return ChecklistResponse.builder()
                .checklistId(checklist.getChecklistId())
                .itemId(checklist.getItemId())
                .title(checklist.getTitle())
                .isCompleted(checklist.getIsCompleted())
                .completedAt(checklist.getCompletedAt())
                .completedBy(checklist.getCompletedBy())
                .completedByName(checklist.getCompletedByName())
                .sortOrder(checklist.getSortOrder())
                .dueDate(checklist.getDueDate())
                .assigneeUsername(checklist.getAssigneeUsername())
                .assigneeName(checklist.getAssigneeName())
                .createdAt(checklist.getCreatedAt())
                .createdBy(checklist.getCreatedBy())
                .updatedAt(checklist.getUpdatedAt())
                .updatedBy(checklist.getUpdatedBy())
                .isOverdue(checklist.isOverdue())
                .isDueToday(checklist.isDueToday())
                .build();
    }
}
