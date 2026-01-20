package com.taskflow.dto.calendar;

import lombok.*;

import java.time.LocalDate;

/**
 * 음력 변환 응답 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LunarConversionResponse {

    private String solar;       // 양력 날짜 (YYYY-MM-DD)
    private LunarDateResponse lunar;  // 음력 날짜 정보

    /**
     * 양력 날짜를 LocalDate로 반환
     */
    public LocalDate getSolarDate() {
        if (solar == null || solar.isEmpty()) {
            return null;
        }
        return LocalDate.parse(solar);
    }

    /**
     * 음력 연도 반환
     */
    public Integer getLunarYear() {
        return lunar != null ? lunar.getYear() : null;
    }

    /**
     * 음력 월 반환
     */
    public Integer getLunarMonth() {
        return lunar != null ? lunar.getMonth() : null;
    }

    /**
     * 음력 일 반환
     */
    public Integer getLunarDay() {
        return lunar != null ? lunar.getDay() : null;
    }

    /**
     * 윤달 여부 반환
     */
    public Boolean getIsLeapMonth() {
        return lunar != null ? lunar.getIsLeapMonth() : null;
    }

    /**
     * 양력 → 음력 변환 결과 생성
     */
    public static LunarConversionResponse fromSolarToLunar(
            LocalDate solarDate,
            int lunarYear, int lunarMonth, int lunarDay, boolean isLeapMonth) {
        return LunarConversionResponse.builder()
                .solar(solarDate.toString())
                .lunar(LunarDateResponse.of(lunarYear, lunarMonth, lunarDay, isLeapMonth))
                .build();
    }

    /**
     * 음력 → 양력 변환 결과 생성
     */
    public static LunarConversionResponse fromLunarToSolar(
            LocalDate solarDate,
            int lunarYear, int lunarMonth, int lunarDay, boolean isLeapMonth) {
        return LunarConversionResponse.builder()
                .solar(solarDate.toString())
                .lunar(LunarDateResponse.of(lunarYear, lunarMonth, lunarDay, isLeapMonth))
                .build();
    }
}
