package com.taskflow.dto.calendar;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Gantt 차트 업무 응답 DTO
 */
@Getter
@Builder
public class GanttItemResponse {

    /**
     * 업무 ID
     */
    private Long itemId;

    /**
     * 업무 제목
     */
    private String title;

    /**
     * 업무 설명
     */
    private String description;

    /**
     * 부모 업무 ID
     */
    private Long parentItemId;

    /**
     * 부모 업무 제목
     */
    private String parentTitle;

    /**
     * 업무 깊이 (0: 기본, 1: 하위, 2: 손자)
     */
    private Integer itemDepth;

    /**
     * 요청일 (기본 컬럼)
     */
    private LocalDate requestDate;

    /**
     * 마감일 (기본 컬럼)
     */
    private LocalDate dueDate;

    /**
     * 시작일 (동적 속성)
     */
    private LocalDate startDate;

    /**
     * 완료일 (동적 속성)
     */
    private LocalDate completionDate;

    /**
     * 시작일 속성 존재 여부
     */
    private Boolean hasStartDateProperty;

    /**
     * 완료일 속성 존재 여부
     */
    private Boolean hasCompletionDateProperty;

    /**
     * 상태: NOT_STARTED, IN_PROGRESS, COMPLETED, DELETED
     */
    private String status;

    /**
     * 우선순위: URGENT, HIGH, NORMAL, LOW
     */
    private String priority;

    /**
     * 담당자명
     */
    private String assigneeUserName;

    /**
     * 보드 ID
     */
    private Long boardId;

    /**
     * 보드명
     */
    private String boardName;

    /**
     * 보드 소유자명
     */
    private String boardOwnerName;

    /**
     * 생성일시
     */
    private LocalDateTime createdAt;

    // ========== 공유/배당 관련 필드 ==========

    /**
     * 현재 사용자에게 공유된 업무 여부
     */
    private Boolean isSharedToMe;

    /**
     * 공유해준 사용자 이름
     */
    private String sharedByUserName;

    /**
     * 현재 사용자에게 배당된 업무 여부
     */
    private Boolean isAssignedToMe;

    /**
     * 배당해준 사용자 이름
     */
    private String assignedByUserName;

    /**
     * 보드 공유 여부 (보드 레벨)
     */
    private Boolean isBoardShared;

    /**
     * 업무 공유 여부 (업무 레벨)
     */
    private Boolean isItemShared;
}
