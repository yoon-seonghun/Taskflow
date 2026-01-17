package com.taskflow.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 개인 Todo 엔티티
 *
 * 테이블: TB_TODO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Todo {

    /**
     * Todo ID (PK)
     */
    private Long todoId;

    /**
     * 소유자 USER_ID (FK → TB_USER)
     */
    private Long ownerUserId;

    /**
     * Todo 제목
     */
    private String title;

    /**
     * 메모/상세 설명
     */
    private String description;

    /**
     * 우선순위 (URGENT/HIGH/NORMAL/LOW)
     */
    private String priority;

    /**
     * 마감일
     */
    private LocalDate dueDate;

    /**
     * 마감 시간
     */
    private LocalTime dueTime;

    /**
     * 완료 여부
     */
    private Boolean isCompleted;

    /**
     * 완료 일시
     */
    private LocalDateTime completedAt;

    /**
     * 정렬 순서
     */
    private Integer sortOrder;

    /**
     * 반복 유형 (NONE/DAILY/WEEKLY/MONTHLY/YEARLY)
     */
    private String repeatType;

    /**
     * 반복 간격
     */
    private Integer repeatInterval;

    /**
     * 반복 요일 (1,2,3,4,5)
     */
    private String repeatDays;

    /**
     * 반복 종료일
     */
    private LocalDate repeatEndDate;

    /**
     * 다음 반복 마감일
     */
    private LocalDate nextDueDate;

    /**
     * 반복 원본 TODO_ID (FK → TB_TODO)
     */
    private Long parentTodoId;

    /**
     * 이관 출처 사용자 (FK → TB_USER)
     */
    private Long transferredFromUserId;

    /**
     * 이관 일시
     */
    private LocalDateTime transferredAt;

    /**
     * 사용 여부
     */
    private String useYn;

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
     * 소유자명
     */
    private String ownerUserName;

    /**
     * 소유자 로그인ID
     */
    private String ownerUsername;

    /**
     * 이관 출처 사용자명
     */
    private String transferredFromUserName;

    /**
     * 공유 수
     */
    private Integer shareCount;

    // =============================================
    // 상수
    // =============================================

    public static final String PRIORITY_URGENT = "URGENT";
    public static final String PRIORITY_HIGH = "HIGH";
    public static final String PRIORITY_NORMAL = "NORMAL";
    public static final String PRIORITY_LOW = "LOW";

    public static final String REPEAT_NONE = "NONE";
    public static final String REPEAT_DAILY = "DAILY";
    public static final String REPEAT_WEEKLY = "WEEKLY";
    public static final String REPEAT_MONTHLY = "MONTHLY";
    public static final String REPEAT_YEARLY = "YEARLY";

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
     * 반복 Todo 여부
     */
    public boolean hasRepeat() {
        return repeatType != null && !REPEAT_NONE.equals(repeatType);
    }

    /**
     * 이관받은 Todo 여부
     */
    public boolean isTransferred() {
        return transferredFromUserId != null;
    }

    /**
     * 반복 생성된 Todo 여부
     */
    public boolean isRepeatedInstance() {
        return parentTodoId != null;
    }

    /**
     * 우선순위 레벨 (정렬용)
     */
    public int getPriorityLevel() {
        if (PRIORITY_URGENT.equals(priority)) return 4;
        if (PRIORITY_HIGH.equals(priority)) return 3;
        if (PRIORITY_NORMAL.equals(priority)) return 2;
        if (PRIORITY_LOW.equals(priority)) return 1;
        return 0;
    }

    /**
     * 지연 여부
     */
    public boolean isOverdue() {
        if (Boolean.TRUE.equals(isCompleted)) return false;
        if (dueDate == null) return false;

        LocalDate today = LocalDate.now();
        if (dueDate.isBefore(today)) return true;

        if (dueDate.isEqual(today) && dueTime != null) {
            return dueTime.isBefore(LocalTime.now());
        }

        return false;
    }

    /**
     * 오늘 마감 여부
     */
    public boolean isDueToday() {
        if (dueDate == null) return false;
        return dueDate.isEqual(LocalDate.now());
    }

    // Lombok 호환성 getters
    public Long getTodoId() {
        return todoId;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getPriority() {
        return priority;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public LocalTime getDueTime() {
        return dueTime;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public String getRepeatType() {
        return repeatType;
    }

    public Integer getRepeatInterval() {
        return repeatInterval;
    }

    public String getRepeatDays() {
        return repeatDays;
    }

    public LocalDate getRepeatEndDate() {
        return repeatEndDate;
    }

    public LocalDate getNextDueDate() {
        return nextDueDate;
    }

    public Long getParentTodoId() {
        return parentTodoId;
    }

    public Long getTransferredFromUserId() {
        return transferredFromUserId;
    }

    public LocalDateTime getTransferredAt() {
        return transferredAt;
    }

    public String getUseYn() {
        return useYn;
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

    public String getOwnerUserName() {
        return ownerUserName;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public String getTransferredFromUserName() {
        return transferredFromUserName;
    }

    public Integer getShareCount() {
        return shareCount;
    }
}
