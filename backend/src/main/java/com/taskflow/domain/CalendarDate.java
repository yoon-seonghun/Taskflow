package com.taskflow.domain;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 날짜 기준 테이블 도메인
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarDate {

    // 공휴일 유형 상수
    public static final String HOLIDAY_SOLAR_FIXED = "SOLAR_FIXED";      // 양력 고정 공휴일
    public static final String HOLIDAY_LUNAR_FIXED = "LUNAR_FIXED";      // 음력 고정 공휴일
    public static final String HOLIDAY_SUBSTITUTE = "SUBSTITUTE";         // 대체공휴일
    public static final String HOLIDAY_TEMPORARY = "TEMPORARY";           // 임시공휴일

    // 요일 상수
    public static final int SUNDAY = 1;
    public static final int MONDAY = 2;
    public static final int TUESDAY = 3;
    public static final int WEDNESDAY = 4;
    public static final int THURSDAY = 5;
    public static final int FRIDAY = 6;
    public static final int SATURDAY = 7;

    // PK
    private LocalDate calDate;

    // 양력 정보
    private Integer solarYear;
    private Integer solarMonth;
    private Integer solarDay;
    private Integer dayOfWeek;          // 1=일, 2=월, ..., 7=토
    private String dayOfWeekName;       // 일, 월, 화, ...
    private Integer weekOfYear;         // 연중 주차
    private Integer weekOfMonth;        // 월중 주차
    private Integer quarter;            // 분기 (1-4)

    // 음력 정보
    private Integer lunarYear;
    private Integer lunarMonth;
    private Integer lunarDay;
    private Boolean isLeapMonth;
    private String lunarDisplay;        // "12.5" 또는 "윤4.1"

    // 근무일/휴일 정보
    private Boolean isHoliday;
    private String holidayName;
    private String holidayType;         // SOLAR_FIXED, LUNAR_FIXED, SUBSTITUTE, TEMPORARY
    private Boolean isWorkday;

    // 감사 필드
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * 주말 여부
     */
    public boolean isWeekend() {
        return dayOfWeek == SUNDAY || dayOfWeek == SATURDAY;
    }

    /**
     * 평일 여부 (주말 아님)
     */
    public boolean isWeekday() {
        return !isWeekend();
    }

    /**
     * 요일명 (한글)
     */
    public static String getDayOfWeekName(int dayOfWeek) {
        return switch (dayOfWeek) {
            case SUNDAY -> "일";
            case MONDAY -> "월";
            case TUESDAY -> "화";
            case WEDNESDAY -> "수";
            case THURSDAY -> "목";
            case FRIDAY -> "금";
            case SATURDAY -> "토";
            default -> "";
        };
    }
}
