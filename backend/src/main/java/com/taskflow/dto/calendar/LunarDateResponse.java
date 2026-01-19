package com.taskflow.dto.calendar;

import lombok.*;

/**
 * 음력 날짜 응답 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LunarDateResponse {

    private Integer year;
    private Integer month;
    private Integer day;
    private Boolean isLeapMonth;
    private String displayText;  // "12.5" 또는 "윤4.1"

    /**
     * 표시용 텍스트 생성
     */
    public static LunarDateResponse of(int year, int month, int day, boolean isLeapMonth) {
        String displayText = (isLeapMonth ? "윤" : "") + month + "." + day;
        return LunarDateResponse.builder()
                .year(year)
                .month(month)
                .day(day)
                .isLeapMonth(isLeapMonth)
                .displayText(displayText)
                .build();
    }
}
