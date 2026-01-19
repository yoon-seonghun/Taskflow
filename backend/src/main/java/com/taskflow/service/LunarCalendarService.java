package com.taskflow.service;

import com.taskflow.dto.calendar.LunarConversionResponse;
import com.taskflow.dto.calendar.LunarDateResponse;
import com.taskflow.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * 한국 음력 달력 서비스
 * 양력 ↔ 음력 변환 기능 제공 (1900-2050년 범위)
 */
@Slf4j
@Service
public class LunarCalendarService {

    // 기준일: 1900년 1월 31일 = 음력 1900년 1월 1일
    private static final LocalDate BASE_SOLAR_DATE = LocalDate.of(1900, 1, 31);
    private static final int BASE_LUNAR_YEAR = 1900;

    // 음력 데이터 (1900-2050년)
    // 각 연도의 음력 정보를 16진수로 인코딩
    // 비트 구조: [윤달위치(4bit)][12개월의 대소(12bit)] + 윤달 대소(별도 저장)
    private static final int[] LUNAR_DATA = {
        0x04bd8, 0x04ae0, 0x0a570, 0x054d5, 0x0d260, 0x0d950, 0x16554, 0x056a0, 0x09ad0, 0x055d2,  // 1900-1909
        0x04ae0, 0x0a5b6, 0x0a4d0, 0x0d250, 0x1d255, 0x0b540, 0x0d6a0, 0x0ada2, 0x095b0, 0x14977,  // 1910-1919
        0x04970, 0x0a4b0, 0x0b4b5, 0x06a50, 0x06d40, 0x1ab54, 0x02b60, 0x09570, 0x052f2, 0x04970,  // 1920-1929
        0x06566, 0x0d4a0, 0x0ea50, 0x06e95, 0x05ad0, 0x02b60, 0x186e3, 0x092e0, 0x1c8d7, 0x0c950,  // 1930-1939
        0x0d4a0, 0x1d8a6, 0x0b550, 0x056a0, 0x1a5b4, 0x025d0, 0x092d0, 0x0d2b2, 0x0a950, 0x0b557,  // 1940-1949
        0x06ca0, 0x0b550, 0x15355, 0x04da0, 0x0a5b0, 0x14573, 0x052b0, 0x0a9a8, 0x0e950, 0x06aa0,  // 1950-1959
        0x0aea6, 0x0ab50, 0x04b60, 0x0aae4, 0x0a570, 0x05260, 0x0f263, 0x0d950, 0x05b57, 0x056a0,  // 1960-1969
        0x096d0, 0x04dd5, 0x04ad0, 0x0a4d0, 0x0d4d4, 0x0d250, 0x0d558, 0x0b540, 0x0b6a0, 0x195a6,  // 1970-1979
        0x095b0, 0x049b0, 0x0a974, 0x0a4b0, 0x0b27a, 0x06a50, 0x06d40, 0x0af46, 0x0ab60, 0x09570,  // 1980-1989
        0x04af5, 0x04970, 0x064b0, 0x074a3, 0x0ea50, 0x06b58, 0x05ac0, 0x0ab60, 0x096d5, 0x092e0,  // 1990-1999
        0x0c960, 0x0d954, 0x0d4a0, 0x0da50, 0x07552, 0x056a0, 0x0abb7, 0x025d0, 0x092d0, 0x0cab5,  // 2000-2009
        0x0a950, 0x0b4a0, 0x0baa4, 0x0ad50, 0x055d9, 0x04ba0, 0x0a5b0, 0x15176, 0x052b0, 0x0a930,  // 2010-2019
        0x07954, 0x06aa0, 0x0ad50, 0x05b52, 0x04b60, 0x0a6e6, 0x0a4e0, 0x0d260, 0x0ea65, 0x0d530,  // 2020-2029
        0x05aa0, 0x076a3, 0x096d0, 0x04afb, 0x04ad0, 0x0a4d0, 0x1d0b6, 0x0d250, 0x0d520, 0x0dd45,  // 2030-2039
        0x0b5a0, 0x056d0, 0x055b2, 0x049b0, 0x0a577, 0x0a4b0, 0x0aa50, 0x1b255, 0x06d20, 0x0ada0,  // 2040-2049
        0x14b63                                                                                     // 2050
    };

    // 월별 음력 데이터 캐시
    private final Map<String, LunarDateResponse> solarToLunarCache = new HashMap<>();
    private final Map<String, LocalDate> lunarToSolarCache = new HashMap<>();

    /**
     * 양력 → 음력 변환
     */
    public LunarConversionResponse solarToLunar(LocalDate solarDate) {
        validateSolarDate(solarDate);

        String cacheKey = solarDate.toString();
        LunarDateResponse cached = solarToLunarCache.get(cacheKey);
        if (cached != null) {
            return LunarConversionResponse.builder()
                    .solar(solarDate.toString())
                    .lunar(cached)
                    .build();
        }

        // 기준일로부터의 일수 계산
        long offset = ChronoUnit.DAYS.between(BASE_SOLAR_DATE, solarDate);

        int lunarYear = BASE_LUNAR_YEAR;
        int lunarMonth = 1;
        int lunarDay;
        boolean isLeapMonth = false;

        // 연도 찾기
        int yearDays;
        while (lunarYear <= 2050 && offset >= (yearDays = getLunarYearDays(lunarYear))) {
            offset -= yearDays;
            lunarYear++;
        }

        // 월 찾기
        int leapMonth = getLeapMonth(lunarYear);
        boolean isLeapYear = leapMonth > 0;

        for (int m = 1; m <= 12; m++) {
            int monthDays;

            // 윤달 처리
            if (isLeapYear && m == leapMonth + 1 && !isLeapMonth) {
                // 윤달 확인
                monthDays = getLeapMonthDays(lunarYear);
                isLeapMonth = true;
                m--; // 월 번호 유지
            } else {
                monthDays = getLunarMonthDays(lunarYear, m);
                isLeapMonth = false;
            }

            if (offset < monthDays) {
                lunarMonth = m;
                break;
            }
            offset -= monthDays;
        }

        lunarDay = (int) offset + 1;

        LunarDateResponse lunar = LunarDateResponse.of(lunarYear, lunarMonth, lunarDay, isLeapMonth);
        solarToLunarCache.put(cacheKey, lunar);

        return LunarConversionResponse.builder()
                .solar(solarDate.toString())
                .lunar(lunar)
                .build();
    }

    /**
     * 음력 → 양력 변환
     */
    public LunarConversionResponse lunarToSolar(int lunarYear, int lunarMonth, int lunarDay, boolean isLeapMonth) {
        validateLunarDate(lunarYear, lunarMonth, lunarDay, isLeapMonth);

        String cacheKey = String.format("%d-%d-%d-%s", lunarYear, lunarMonth, lunarDay, isLeapMonth);
        LocalDate cached = lunarToSolarCache.get(cacheKey);
        if (cached != null) {
            return LunarConversionResponse.fromLunarToSolar(cached, lunarYear, lunarMonth, lunarDay, isLeapMonth);
        }

        // 기준일로부터의 일수 계산
        long offset = 0;

        // 연도 계산
        for (int y = BASE_LUNAR_YEAR; y < lunarYear; y++) {
            offset += getLunarYearDays(y);
        }

        // 월 계산
        int leapMonth = getLeapMonth(lunarYear);
        for (int m = 1; m < lunarMonth; m++) {
            offset += getLunarMonthDays(lunarYear, m);
            if (m == leapMonth) {
                offset += getLeapMonthDays(lunarYear);
            }
        }

        // 윤달인 경우 해당 월도 추가
        if (isLeapMonth) {
            offset += getLunarMonthDays(lunarYear, lunarMonth);
        }

        // 일 계산
        offset += lunarDay - 1;

        LocalDate solarDate = BASE_SOLAR_DATE.plusDays(offset);
        lunarToSolarCache.put(cacheKey, solarDate);

        return LunarConversionResponse.fromLunarToSolar(solarDate, lunarYear, lunarMonth, lunarDay, isLeapMonth);
    }

    /**
     * 음력 날짜 유효성 검증
     */
    public boolean isValidLunarDate(int year, int month, int day, boolean isLeapMonth) {
        if (year < 1900 || year > 2050) return false;
        if (month < 1 || month > 12) return false;
        if (day < 1 || day > 30) return false;

        // 윤달 존재 여부 확인
        if (isLeapMonth && getLeapMonth(year) != month) {
            return false;
        }

        // 해당 월의 일수 확인
        int monthDays = isLeapMonth ? getLeapMonthDays(year) : getLunarMonthDays(year, month);
        return day <= monthDays;
    }

    /**
     * 특정 연도의 윤달 월 반환 (0이면 윤달 없음)
     */
    public int getLeapMonth(int year) {
        if (year < 1900 || year > 2050) return 0;
        int data = LUNAR_DATA[year - 1900];
        return data & 0xf;
    }

    /**
     * 음력 해당 월의 일수 (대월:30, 소월:29)
     */
    public int getLunarMonthDays(int year, int month) {
        if (year < 1900 || year > 2050 || month < 1 || month > 12) return 0;
        int data = LUNAR_DATA[year - 1900];
        return ((data >> (16 - month)) & 0x1) == 1 ? 30 : 29;
    }

    /**
     * 음력 해당 연도 윤달의 일수
     */
    public int getLeapMonthDays(int year) {
        if (year < 1900 || year > 2050) return 0;
        int leapMonth = getLeapMonth(year);
        if (leapMonth == 0) return 0;

        int data = LUNAR_DATA[year - 1900];
        return ((data >> 16) & 0x1) == 1 ? 30 : 29;
    }

    /**
     * 음력 해당 연도의 총 일수
     */
    public int getLunarYearDays(int year) {
        if (year < 1900 || year > 2050) return 0;

        int days = 0;
        for (int m = 1; m <= 12; m++) {
            days += getLunarMonthDays(year, m);
        }

        // 윤달 일수 추가
        int leapMonth = getLeapMonth(year);
        if (leapMonth > 0) {
            days += getLeapMonthDays(year);
        }

        return days;
    }

    /**
     * 해당 월에 윤달이 있는지 확인
     */
    public boolean hasLeapMonth(int year, int month) {
        return getLeapMonth(year) == month;
    }

    /**
     * 양력 날짜 유효성 검증
     */
    private void validateSolarDate(LocalDate date) {
        if (date.getYear() < 1900 || date.getYear() > 2050) {
            throw new BusinessException("지원 범위를 벗어난 날짜입니다 (1900-2050년)");
        }
    }

    /**
     * 음력 날짜 유효성 검증
     */
    private void validateLunarDate(int year, int month, int day, boolean isLeapMonth) {
        if (year < 1900 || year > 2050) {
            throw new BusinessException("지원 범위를 벗어난 연도입니다 (1900-2050년)");
        }
        if (month < 1 || month > 12) {
            throw new BusinessException("유효하지 않은 월입니다 (1-12)");
        }
        if (day < 1 || day > 30) {
            throw new BusinessException("유효하지 않은 일입니다 (1-30)");
        }
        if (isLeapMonth && getLeapMonth(year) != month) {
            throw new BusinessException(String.format("%d년 %d월은 윤달이 아닙니다", year, month));
        }

        int monthDays = isLeapMonth ? getLeapMonthDays(year) : getLunarMonthDays(year, month);
        if (day > monthDays) {
            throw new BusinessException(String.format("%d년 %s%d월은 %d일까지입니다",
                    year, isLeapMonth ? "윤" : "", month, monthDays));
        }
    }

    /**
     * 캐시 초기화
     */
    public void clearCache() {
        solarToLunarCache.clear();
        lunarToSolarCache.clear();
    }
}
