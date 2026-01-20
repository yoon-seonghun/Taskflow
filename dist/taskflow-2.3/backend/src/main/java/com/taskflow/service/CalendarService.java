package com.taskflow.service;

import com.taskflow.dto.calendar.CalendarResponse;
import com.taskflow.dto.calendar.GanttResponse;

/**
 * Calendar/Gantt 서비스 인터페이스
 */
public interface CalendarService {

    /**
     * 캘린더 데이터 조회
     *
     * @param username 사용자명
     * @param userId 사용자 ID (Todo 조회용)
     * @param year 연도
     * @param month 월
     * @param includeTodo Todo 포함 여부
     * @param includeItem Item 포함 여부
     * @param boardId 보드 ID (optional)
     * @return 캘린더 응답
     */
    CalendarResponse getCalendarData(
            String username,
            Long userId,
            int year,
            int month,
            boolean includeTodo,
            boolean includeItem,
            Long boardId
    );

    /**
     * Gantt 차트 데이터 조회
     *
     * @param username 사용자명
     * @param boardId 보드 ID (optional)
     * @param range 범위: FULL, WEEK, MONTH, CUSTOM
     * @param startDate 시작일 (CUSTOM 범위용)
     * @param endDate 종료일 (CUSTOM 범위용)
     * @param includeCompleted 완료 포함 여부
     * @return Gantt 응답
     */
    GanttResponse getGanttData(
            String username,
            Long boardId,
            String range,
            String startDate,
            String endDate,
            boolean includeCompleted
    );
}
