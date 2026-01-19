package com.taskflow.dto.calendar;

import com.taskflow.domain.CalendarDate;
import lombok.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 날짜 기준 데이터 응답 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarDateResponse {

    // 양력 정보
    private LocalDate calDate;
    private Integer solarYear;
    private Integer solarMonth;
    private Integer solarDay;
    private Integer dayOfWeek;
    private String dayOfWeekName;
    private Integer weekOfYear;
    private Integer weekOfMonth;
    private Integer quarter;

    // 음력 정보
    private Integer lunarYear;
    private Integer lunarMonth;
    private Integer lunarDay;
    private Boolean isLeapMonth;
    private String lunarDisplay;

    // 근무일/휴일 정보
    private Boolean isHoliday;
    private String holidayName;
    private String holidayType;
    private Boolean isWorkday;
    private Boolean isWeekend;

    /**
     * Domain → DTO 변환
     */
    public static CalendarDateResponse from(CalendarDate entity) {
        if (entity == null) return null;

        return CalendarDateResponse.builder()
                .calDate(entity.getCalDate())
                .solarYear(entity.getSolarYear())
                .solarMonth(entity.getSolarMonth())
                .solarDay(entity.getSolarDay())
                .dayOfWeek(entity.getDayOfWeek())
                .dayOfWeekName(entity.getDayOfWeekName())
                .weekOfYear(entity.getWeekOfYear())
                .weekOfMonth(entity.getWeekOfMonth())
                .quarter(entity.getQuarter())
                .lunarYear(entity.getLunarYear())
                .lunarMonth(entity.getLunarMonth())
                .lunarDay(entity.getLunarDay())
                .isLeapMonth(entity.getIsLeapMonth())
                .lunarDisplay(entity.getLunarDisplay())
                .isHoliday(entity.getIsHoliday())
                .holidayName(entity.getHolidayName())
                .holidayType(entity.getHolidayType())
                .isWorkday(entity.getIsWorkday())
                .isWeekend(entity.isWeekend())
                .build();
    }

    /**
     * Domain List → DTO List 변환
     */
    public static List<CalendarDateResponse> fromList(List<CalendarDate> entities) {
        if (entities == null) return List.of();
        return entities.stream()
                .map(CalendarDateResponse::from)
                .collect(Collectors.toList());
    }
}
