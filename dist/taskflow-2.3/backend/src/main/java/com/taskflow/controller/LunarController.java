package com.taskflow.controller;

import com.taskflow.common.ApiResponse;
import com.taskflow.dto.calendar.LunarConversionResponse;
import com.taskflow.dto.calendar.LunarDateResponse;
import com.taskflow.service.LunarCalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 음력 변환 컨트롤러
 *
 * API:
 * - GET /api/calendar/lunar       - 양력 → 음력 변환
 * - GET /api/calendar/solar       - 음력 → 양력 변환
 * - GET /api/calendar/lunar/month - 월간 음력 데이터 조회
 * - GET /api/calendar/lunar/valid - 음력 날짜 유효성 검증
 */
@Slf4j
@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
public class LunarController {

    private final LunarCalendarService lunarService;

    /**
     * 양력 → 음력 변환
     *
     * @param solarDate 양력 날짜 (YYYY-MM-DD)
     */
    @GetMapping("/lunar")
    public ResponseEntity<ApiResponse<LunarConversionResponse>> solarToLunar(
            @RequestParam("solar") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate solarDate
    ) {
        log.debug("Solar to lunar conversion: {}", solarDate);
        LunarConversionResponse result = lunarService.solarToLunar(solarDate);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 음력 → 양력 변환
     *
     * @param year 음력 연도
     * @param month 음력 월
     * @param day 음력 일
     * @param leapMonth 윤달 여부
     */
    @GetMapping("/solar")
    public ResponseEntity<ApiResponse<LunarConversionResponse>> lunarToSolar(
            @RequestParam("year") Integer year,
            @RequestParam("month") Integer month,
            @RequestParam("day") Integer day,
            @RequestParam(value = "leapMonth", required = false, defaultValue = "false") Boolean leapMonth
    ) {
        log.debug("Lunar to solar conversion: {}-{}-{} (leap: {})", year, month, day, leapMonth);
        LunarConversionResponse result = lunarService.lunarToSolar(year, month, day, leapMonth);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 월간 음력 데이터 조회
     * 캘린더 렌더링 최적화용 - 한 달치 음력 데이터를 한 번에 반환
     *
     * @param year 양력 연도
     * @param month 양력 월
     */
    @GetMapping("/lunar/month")
    public ResponseEntity<ApiResponse<Map<String, LunarDateResponse>>> getMonthLunarDates(
            @RequestParam("year") Integer year,
            @RequestParam("month") Integer month
    ) {
        log.debug("Get month lunar dates: {}-{}", year, month);

        Map<String, LunarDateResponse> result = new HashMap<>();

        // 이전 달 일부 + 현재 달 + 다음 달 일부 (42일 분량)
        LocalDate startDate = LocalDate.of(year, month, 1).minusDays(7);
        for (int i = 0; i < 42; i++) {
            LocalDate date = startDate.plusDays(i);
            LunarConversionResponse conversion = lunarService.solarToLunar(date);
            result.put(date.toString(), conversion.getLunar());
        }

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 음력 날짜 유효성 검증
     *
     * @param year 음력 연도
     * @param month 음력 월
     * @param day 음력 일
     * @param leapMonth 윤달 여부
     */
    @GetMapping("/lunar/valid")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateLunarDate(
            @RequestParam("year") Integer year,
            @RequestParam("month") Integer month,
            @RequestParam("day") Integer day,
            @RequestParam(value = "leapMonth", required = false, defaultValue = "false") Boolean leapMonth
    ) {
        log.debug("Validate lunar date: {}-{}-{} (leap: {})", year, month, day, leapMonth);

        Map<String, Object> result = new HashMap<>();
        result.put("year", year);
        result.put("month", month);
        result.put("day", day);
        result.put("isLeapMonth", leapMonth);
        result.put("isValid", lunarService.isValidLunarDate(year, month, day, leapMonth));

        // 해당 연도의 윤달 정보
        int leapMonthOfYear = lunarService.getLeapMonth(year);
        result.put("leapMonthOfYear", leapMonthOfYear > 0 ? leapMonthOfYear : null);

        // 해당 월의 일수
        if (lunarService.isValidLunarDate(year, month, 1, leapMonth)) {
            int monthDays = leapMonth ?
                    lunarService.getLeapMonthDays(year) :
                    lunarService.getLunarMonthDays(year, month);
            result.put("monthDays", monthDays);
        }

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 특정 연도의 윤달 정보 조회
     *
     * @param year 음력 연도
     */
    @GetMapping("/lunar/leap")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLeapMonthInfo(
            @RequestParam("year") Integer year
    ) {
        log.debug("Get leap month info: {}", year);

        Map<String, Object> result = new HashMap<>();
        result.put("year", year);

        int leapMonth = lunarService.getLeapMonth(year);
        result.put("hasLeapMonth", leapMonth > 0);
        result.put("leapMonth", leapMonth > 0 ? leapMonth : null);

        if (leapMonth > 0) {
            result.put("leapMonthDays", lunarService.getLeapMonthDays(year));
        }

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 특정 음력 월의 일수 조회
     *
     * @param year 음력 연도
     * @param month 음력 월
     * @param leapMonth 윤달 여부
     */
    @GetMapping("/lunar/days")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLunarMonthDays(
            @RequestParam("year") Integer year,
            @RequestParam("month") Integer month,
            @RequestParam(value = "leapMonth", required = false, defaultValue = "false") Boolean leapMonth
    ) {
        log.debug("Get lunar month days: {}-{} (leap: {})", year, month, leapMonth);

        Map<String, Object> result = new HashMap<>();
        result.put("year", year);
        result.put("month", month);
        result.put("isLeapMonth", leapMonth);

        int days;
        if (leapMonth) {
            if (lunarService.getLeapMonth(year) != month) {
                result.put("days", 0);
                result.put("error", String.format("%d년 %d월은 윤달이 아닙니다", year, month));
            } else {
                days = lunarService.getLeapMonthDays(year);
                result.put("days", days);
            }
        } else {
            days = lunarService.getLunarMonthDays(year, month);
            result.put("days", days);
        }

        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
