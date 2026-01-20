package com.taskflow.dto.calendar;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Gantt 차트 응답 DTO
 */
@Getter
@Builder
public class GanttResponse {

    /**
     * 날짜 범위
     */
    private GanttDateRange dateRange;

    /**
     * 시작일/완료일 속성 정보
     */
    private GanttPropertyInfo propertyInfo;

    /**
     * 업무 목록
     */
    private List<GanttItemResponse> items;
}
