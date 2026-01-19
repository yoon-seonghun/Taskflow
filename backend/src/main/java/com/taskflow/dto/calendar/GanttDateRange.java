package com.taskflow.dto.calendar;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/**
 * Gantt 날짜 범위 DTO
 */
@Getter
@Builder
public class GanttDateRange {

    /**
     * 시작 날짜
     */
    private LocalDate start;

    /**
     * 종료 날짜
     */
    private LocalDate end;
}
