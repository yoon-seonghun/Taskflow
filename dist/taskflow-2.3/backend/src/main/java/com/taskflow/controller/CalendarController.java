package com.taskflow.controller;

import com.taskflow.common.ApiResponse;
import com.taskflow.dto.calendar.CalendarResponse;
import com.taskflow.dto.calendar.GanttResponse;
import com.taskflow.security.SecurityUtils;
import com.taskflow.service.CalendarService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Calendar/Gantt 컨트롤러
 *
 * API:
 * - GET /api/calendar - 캘린더 데이터 조회
 * - GET /api/gantt - Gantt 차트 데이터 조회
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CalendarController {

    private final CalendarService calendarService;

    /**
     * 캘린더 데이터 조회
     *
     * @param year 연도
     * @param month 월
     * @param includeTodo Todo 포함 여부 (기본: true)
     * @param includeItem Item 포함 여부 (기본: true)
     * @param boardId 보드 ID (optional - 특정 보드만 조회)
     * @return 캘린더 응답
     */
    @GetMapping("/calendar")
    public ResponseEntity<ApiResponse<CalendarResponse>> getCalendarData(
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "month", required = false) Integer month,
            @RequestParam(value = "includeTodo", required = false, defaultValue = "true") Boolean includeTodo,
            @RequestParam(value = "includeItem", required = false, defaultValue = "true") Boolean includeItem,
            @RequestParam(value = "boardId", required = false) Long boardId
    ) {
        String username = SecurityUtils.getCurrentUsername();
        Long userId = SecurityUtils.getCurrentUserId();

        // 연/월 기본값 설정 (현재 날짜)
        LocalDate today = LocalDate.now();
        if (year == null) {
            year = today.getYear();
        }
        if (month == null) {
            month = today.getMonthValue();
        }

        log.debug("Get calendar data: username={}, year={}, month={}, includeTodo={}, includeItem={}, boardId={}",
                username, year, month, includeTodo, includeItem, boardId);

        CalendarResponse response = calendarService.getCalendarData(
                username, userId, year, month, includeTodo, includeItem, boardId
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Gantt 차트 데이터 조회
     *
     * @param boardId 보드 ID (optional - 특정 보드만 조회)
     * @param range 범위: FULL(전체), WEEK(주간), MONTH(월간), CUSTOM(사용자 지정)
     * @param startDate 시작일 (CUSTOM 범위용, yyyy-MM-dd)
     * @param endDate 종료일 (CUSTOM 범위용, yyyy-MM-dd)
     * @param includeCompleted 완료 포함 여부 (기본: false)
     * @return Gantt 응답
     */
    @GetMapping("/gantt")
    public ResponseEntity<ApiResponse<GanttResponse>> getGanttData(
            @RequestParam(value = "boardId", required = false) Long boardId,
            @RequestParam(value = "range", required = false, defaultValue = "MONTH") String range,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @RequestParam(value = "includeCompleted", required = false, defaultValue = "false") Boolean includeCompleted
    ) {
        String username = SecurityUtils.getCurrentUsername();

        log.debug("Get gantt data: username={}, boardId={}, range={}, startDate={}, endDate={}, includeCompleted={}",
                username, boardId, range, startDate, endDate, includeCompleted);

        // CUSTOM 범위인데 날짜가 없으면 MONTH로 fallback
        if ("CUSTOM".equalsIgnoreCase(range) && (startDate == null || endDate == null)) {
            range = "MONTH";
        }

        GanttResponse response = calendarService.getGanttData(
                username, boardId, range, startDate, endDate, includeCompleted
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
