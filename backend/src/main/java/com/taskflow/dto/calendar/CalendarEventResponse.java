package com.taskflow.dto.calendar;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/**
 * Calendar 이벤트 응답 DTO
 * Todo와 Item을 통합하여 캘린더에 표시
 */
@Getter
@Builder
public class CalendarEventResponse {

    /**
     * 이벤트 타입: TODO, ITEM
     */
    private String type;

    /**
     * ID (todoId 또는 itemId)
     */
    private Long id;

    /**
     * 제목
     */
    private String title;

    /**
     * 우선순위: URGENT, HIGH, NORMAL, LOW
     */
    private String priority;

    /**
     * 완료 여부 (Todo용)
     */
    private Boolean isCompleted;

    /**
     * 상태 (Item용): NOT_STARTED, IN_PROGRESS, COMPLETED, DELETED
     */
    private String status;

    /**
     * 보드 ID (Item용)
     */
    private Long boardId;

    /**
     * 보드명 (Item용)
     */
    private String boardName;

    /**
     * 날짜 (마감일 기준)
     */
    private LocalDate date;

    /**
     * 부모 업무 ID (하위 업무용)
     */
    private Long parentItemId;

    /**
     * 업무 깊이 (0: 기본, 1: 하위, 2: 손자)
     */
    private Integer itemDepth;
}
