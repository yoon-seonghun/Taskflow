package com.taskflow.dto.calendar;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Calendar 응답 DTO
 * 날짜별 이벤트 목록을 Map 형태로 반환
 */
@Getter
@Builder
public class CalendarResponse {

    /**
     * 날짜별 이벤트 목록
     * key: 날짜 문자열 (yyyy-MM-dd)
     * value: 해당 날짜의 이벤트 목록
     */
    private Map<String, List<CalendarEventResponse>> events;

    /**
     * 날짜별 기준 정보 (공휴일, 음력 등)
     * key: 날짜 문자열 (yyyy-MM-dd)
     * value: 해당 날짜의 기준 정보
     */
    private Map<String, CalendarDateResponse> dateInfo;

    /**
     * 조회 연도
     */
    private Integer year;

    /**
     * 조회 월
     */
    private Integer month;
}
