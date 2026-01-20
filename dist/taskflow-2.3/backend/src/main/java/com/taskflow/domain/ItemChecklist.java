package com.taskflow.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 업무 내 체크리스트 엔티티
 *
 * 테이블: TB_ITEM_CHECKLIST
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemChecklist {

    /**
     * 체크리스트 ID (PK)
     */
    private Long checklistId;

    /**
     * 업무 ID (FK → TB_ITEM)
     */
    private Long itemId;

    /**
     * 체크리스트 항목
     */
    private String title;

    /**
     * 완료 여부
     */
    private Boolean isCompleted;

    /**
     * 완료 일시
     */
    private LocalDateTime completedAt;

    /**
     * 완료자 USERNAME
     */
    private String completedBy;

    /**
     * 정렬 순서
     */
    private Integer sortOrder;

    /**
     * 마감일
     */
    private LocalDate dueDate;

    /**
     * 담당자 USERNAME
     */
    private String assigneeUsername;

    /**
     * 생성일시
     */
    private LocalDateTime createdAt;

    /**
     * 생성자 USERNAME
     */
    private String createdBy;

    /**
     * 수정일시
     */
    private LocalDateTime updatedAt;

    /**
     * 수정자 USERNAME
     */
    private String updatedBy;

    // =============================================
    // 추가 필드 (Mapper에서 JOIN으로 설정)
    // =============================================

    /**
     * 담당자명
     */
    private String assigneeName;

    /**
     * 완료자명
     */
    private String completedByName;

    /**
     * 업무 제목
     */
    private String itemTitle;

    // =============================================
    // 편의 메서드
    // =============================================

    /**
     * 완료 여부 getter (Lombok 호환성)
     */
    public Boolean getIsCompleted() {
        return isCompleted;
    }

    /**
     * 지연 여부
     */
    public boolean isOverdue() {
        if (Boolean.TRUE.equals(isCompleted)) return false;
        if (dueDate == null) return false;
        return dueDate.isBefore(LocalDate.now());
    }

    /**
     * 오늘 마감 여부
     */
    public boolean isDueToday() {
        if (dueDate == null) return false;
        return dueDate.isEqual(LocalDate.now());
    }

    // Lombok 호환성 getters
    public Long getChecklistId() {
        return checklistId;
    }

    public Long getItemId() {
        return itemId;
    }

    public String getTitle() {
        return title;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public String getCompletedBy() {
        return completedBy;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public String getAssigneeUsername() {
        return assigneeUsername;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public String getAssigneeName() {
        return assigneeName;
    }

    public String getCompletedByName() {
        return completedByName;
    }

    public String getItemTitle() {
        return itemTitle;
    }
}
