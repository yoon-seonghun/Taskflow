package com.taskflow.controller;

import com.taskflow.common.ApiResponse;
import com.taskflow.dto.calendar.CalendarDateResponse;
import com.taskflow.service.CalendarDateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 날짜 기준 테이블 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/calendar-dates")
@RequiredArgsConstructor
public class CalendarDateController {

    private final CalendarDateService calendarDateService;

    /**
     * 특정 날짜 조회
     */
    @GetMapping("/date/{date}")
    public ResponseEntity<ApiResponse<CalendarDateResponse>> getByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        CalendarDateResponse response = calendarDateService.getByDate(date);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 기간 조회
     */
    @GetMapping("/range")
    public ResponseEntity<ApiResponse<List<CalendarDateResponse>>> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<CalendarDateResponse> response = calendarDateService.getByDateRange(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 월간 조회
     */
    @GetMapping("/month")
    public ResponseEntity<ApiResponse<List<CalendarDateResponse>>> getByMonth(
            @RequestParam int year,
            @RequestParam int month) {
        List<CalendarDateResponse> response = calendarDateService.getByMonth(year, month);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 연간 조회
     */
    @GetMapping("/year/{year}")
    public ResponseEntity<ApiResponse<List<CalendarDateResponse>>> getByYear(
            @PathVariable int year) {
        List<CalendarDateResponse> response = calendarDateService.getByYear(year);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 공휴일 목록 조회
     */
    @GetMapping("/holidays")
    public ResponseEntity<ApiResponse<List<CalendarDateResponse>>> getHolidays(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<CalendarDateResponse> response = calendarDateService.getHolidays(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 근무일수 계산
     */
    @GetMapping("/workdays/count")
    public ResponseEntity<ApiResponse<Map<String, Object>>> countWorkdays(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        int count = calendarDateService.countWorkdays(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "startDate", startDate,
                "endDate", endDate,
                "workdays", count
        )));
    }

    /**
     * 음력 날짜로 양력 조회
     */
    @GetMapping("/lunar")
    public ResponseEntity<ApiResponse<List<CalendarDateResponse>>> getByLunarDate(
            @RequestParam int lunarYear,
            @RequestParam int lunarMonth,
            @RequestParam int lunarDay,
            @RequestParam(required = false) Boolean isLeapMonth) {
        List<CalendarDateResponse> response = calendarDateService.getByLunarDate(
                lunarYear, lunarMonth, lunarDay, isLeapMonth);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 데이터 존재 연도 범위 조회
     */
    @GetMapping("/year-range")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> getYearRange() {
        Map<String, Integer> range = calendarDateService.getYearRange();
        return ResponseEntity.ok(ApiResponse.success(range));
    }

    /**
     * 연도별 캘린더 생성 (관리자 전용)
     */
    @PostMapping("/generate/{year}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateYearCalendar(
            @PathVariable int year) {
        log.info("Generating calendar for year: {}", year);
        int count = calendarDateService.generateYearCalendar(year);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "year", year,
                "generatedCount", count
        )));
    }

    /**
     * 다년도 캘린더 생성 (관리자 전용)
     */
    @PostMapping("/generate/range")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<Integer, Integer>>> generateMultiYearCalendar(
            @RequestParam int startYear,
            @RequestParam int endYear) {
        log.info("Generating calendar from {} to {}", startYear, endYear);
        Map<Integer, Integer> result = calendarDateService.generateMultiYearCalendar(startYear, endYear);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 임시 공휴일 추가 (관리자 전용)
     */
    @PostMapping("/holidays/temporary")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> addTemporaryHoliday(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam String holidayName) {
        calendarDateService.addTemporaryHoliday(date, holidayName);
        return ResponseEntity.ok(ApiResponse.success(null, "임시 공휴일이 추가되었습니다."));
    }
}
