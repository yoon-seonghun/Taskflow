package com.taskflow.dto.calendar;

import lombok.Builder;
import lombok.Getter;

/**
 * Gantt 시작일/완료일 속성 정보 DTO
 */
@Getter
@Builder
public class GanttPropertyInfo {

    /**
     * 시작일 속성 ID (TB_PROPERTY_DEF.PROPERTY_ID)
     */
    private Long startDatePropertyId;

    /**
     * 완료일 속성 ID (TB_PROPERTY_DEF.PROPERTY_ID)
     */
    private Long completionDatePropertyId;

    /**
     * 시작일 속성 존재 여부
     */
    private Boolean hasStartDateProperty;

    /**
     * 완료일 속성 존재 여부
     */
    private Boolean hasCompletionDateProperty;
}
