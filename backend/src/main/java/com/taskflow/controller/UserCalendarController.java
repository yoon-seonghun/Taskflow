package com.taskflow.controller;

import com.taskflow.common.ApiResponse;
import com.taskflow.dto.calendar.*;
import com.taskflow.security.SecurityUtils;
import com.taskflow.service.UserCalendarService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 사용자 캘린더 컨트롤러
 *
 * API:
 * - GET    /api/calendars              - 내 캘린더 목록
 * - POST   /api/calendars              - 캘린더 생성
 * - GET    /api/calendars/{id}         - 캘린더 상세
 * - PUT    /api/calendars/{id}         - 캘린더 수정
 * - DELETE /api/calendars/{id}         - 캘린더 삭제
 * - GET    /api/calendars/{id}/shares  - 공유 목록
 * - POST   /api/calendars/{id}/shares  - 공유 추가
 * - DELETE /api/calendars/{id}/shares/{username} - 공유 해제
 */
@Slf4j
@RestController
@RequestMapping("/api/calendars")
@RequiredArgsConstructor
public class UserCalendarController {

    private final UserCalendarService calendarService;

    /**
     * 내 캘린더 목록 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<UserCalendarResponse>>> getMyCalendars() {
        String username = SecurityUtils.getCurrentUsername();
        log.debug("Get my calendars: username={}", username);

        List<UserCalendarResponse> calendars = calendarService.getMyCalendars(username);
        return ResponseEntity.ok(ApiResponse.success(calendars));
    }

    /**
     * 캘린더 상세 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserCalendarResponse>> getCalendar(@PathVariable("id") Long calendarId) {
        String username = SecurityUtils.getCurrentUsername();
        log.debug("Get calendar: id={}, username={}", calendarId, username);

        UserCalendarResponse calendar = calendarService.getCalendar(calendarId, username);
        return ResponseEntity.ok(ApiResponse.success(calendar));
    }

    /**
     * 캘린더 생성
     */
    @PostMapping
    public ResponseEntity<ApiResponse<UserCalendarResponse>> createCalendar(
            @Valid @RequestBody UserCalendarRequest request
    ) {
        String username = SecurityUtils.getCurrentUsername();
        log.debug("Create calendar: username={}, name={}", username, request.getCalendarName());

        UserCalendarResponse calendar = calendarService.createCalendar(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(calendar));
    }

    /**
     * 캘린더 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserCalendarResponse>> updateCalendar(
            @PathVariable("id") Long calendarId,
            @Valid @RequestBody UserCalendarRequest request
    ) {
        String username = SecurityUtils.getCurrentUsername();
        log.debug("Update calendar: id={}, username={}", calendarId, username);

        UserCalendarResponse calendar = calendarService.updateCalendar(calendarId, request, username);
        return ResponseEntity.ok(ApiResponse.success(calendar));
    }

    /**
     * 캘린더 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCalendar(@PathVariable("id") Long calendarId) {
        String username = SecurityUtils.getCurrentUsername();
        log.debug("Delete calendar: id={}, username={}", calendarId, username);

        calendarService.deleteCalendar(calendarId, username);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // =============================================
    // 캘린더 공유
    // =============================================

    /**
     * 캘린더 공유 목록 조회
     */
    @GetMapping("/{id}/shares")
    public ResponseEntity<ApiResponse<List<CalendarShareResponse>>> getShares(@PathVariable("id") Long calendarId) {
        String username = SecurityUtils.getCurrentUsername();
        log.debug("Get calendar shares: calendarId={}, username={}", calendarId, username);

        List<CalendarShareResponse> shares = calendarService.getShares(calendarId, username);
        return ResponseEntity.ok(ApiResponse.success(shares));
    }

    /**
     * 캘린더 공유 추가
     */
    @PostMapping("/{id}/shares")
    public ResponseEntity<ApiResponse<Void>> addShare(
            @PathVariable("id") Long calendarId,
            @Valid @RequestBody CalendarShareRequest request
    ) {
        String username = SecurityUtils.getCurrentUsername();
        log.debug("Add calendar share: calendarId={}, targetUser={}", calendarId, request.getUsername());

        calendarService.addShare(calendarId, request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(null));
    }

    /**
     * 캘린더 공유 해제
     */
    @DeleteMapping("/{id}/shares/{targetUsername}")
    public ResponseEntity<ApiResponse<Void>> removeShare(
            @PathVariable("id") Long calendarId,
            @PathVariable("targetUsername") String targetUsername
    ) {
        String username = SecurityUtils.getCurrentUsername();
        log.debug("Remove calendar share: calendarId={}, targetUser={}", calendarId, targetUsername);

        calendarService.removeShare(calendarId, targetUsername, username);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 캘린더 공유 권한 변경
     */
    @PutMapping("/{id}/shares/{targetUsername}")
    public ResponseEntity<ApiResponse<Void>> updateShareType(
            @PathVariable("id") Long calendarId,
            @PathVariable("targetUsername") String targetUsername,
            @RequestParam("shareType") String shareType
    ) {
        String username = SecurityUtils.getCurrentUsername();
        log.debug("Update calendar share type: calendarId={}, targetUser={}, shareType={}",
                calendarId, targetUsername, shareType);

        calendarService.updateShareType(calendarId, targetUsername, shareType, username);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
