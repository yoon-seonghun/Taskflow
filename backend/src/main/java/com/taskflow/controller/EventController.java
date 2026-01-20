package com.taskflow.controller;

import com.taskflow.common.ApiResponse;
import com.taskflow.dto.calendar.*;
import com.taskflow.security.SecurityUtils;
import com.taskflow.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 이벤트 컨트롤러
 *
 * API:
 * - GET    /api/events                 - 이벤트 조회 (기간별)
 * - POST   /api/events                 - 이벤트 생성
 * - GET    /api/events/{id}            - 이벤트 상세
 * - PUT    /api/events/{id}            - 이벤트 수정
 * - DELETE /api/events/{id}            - 이벤트 삭제
 * - GET    /api/events/{id}/shares     - 공유 목록
 * - POST   /api/events/{id}/shares     - 공유 추가
 * - DELETE /api/events/{id}/shares/{username} - 공유 해제
 * - POST   /api/events/{id}/transfer   - 이벤트 이관
 */
@Slf4j
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    /**
     * 이벤트 조회 (기간별)
     *
     * @param startDate 시작일
     * @param endDate 종료일
     * @param calendarId 캘린더 ID (optional)
     * @param includeShared 공유받은 이벤트 포함 여부 (기본: true)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<EventDetailResponse>>> getEvents(
            @RequestParam(value = "startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(value = "endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(value = "calendarId", required = false) Long calendarId,
            @RequestParam(value = "includeShared", required = false, defaultValue = "true") Boolean includeShared,
            @RequestParam(value = "groupId", required = false) Long groupId
    ) {
        String username = SecurityUtils.getCurrentUsername();
        log.debug("Get events: username={}, startDate={}, endDate={}, calendarId={}, includeShared={}, groupId={}",
                username, startDate, endDate, calendarId, includeShared, groupId);

        List<EventDetailResponse> events = eventService.getEventsByDateRange(
                username, startDate, endDate, calendarId, includeShared, groupId
        );
        return ResponseEntity.ok(ApiResponse.success(events));
    }

    /**
     * 이벤트 상세 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventDetailResponse>> getEvent(@PathVariable("id") Long eventId) {
        String username = SecurityUtils.getCurrentUsername();
        log.debug("Get event: id={}, username={}", eventId, username);

        EventDetailResponse event = eventService.getEvent(eventId, username);
        return ResponseEntity.ok(ApiResponse.success(event));
    }

    /**
     * 이벤트 생성
     */
    @PostMapping
    public ResponseEntity<ApiResponse<EventDetailResponse>> createEvent(
            @Valid @RequestBody EventRequest request
    ) {
        String username = SecurityUtils.getCurrentUsername();
        log.debug("Create event: username={}, title={}, calendarId={}",
                username, request.getTitle(), request.getCalendarId());

        EventDetailResponse event = eventService.createEvent(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(event));
    }

    /**
     * 이벤트 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<EventDetailResponse>> updateEvent(
            @PathVariable("id") Long eventId,
            @Valid @RequestBody EventRequest request
    ) {
        String username = SecurityUtils.getCurrentUsername();
        log.debug("Update event: id={}, username={}", eventId, username);

        EventDetailResponse event = eventService.updateEvent(eventId, request, username);
        return ResponseEntity.ok(ApiResponse.success(event));
    }

    /**
     * 이벤트 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteEvent(@PathVariable("id") Long eventId) {
        String username = SecurityUtils.getCurrentUsername();
        log.debug("Delete event: id={}, username={}", eventId, username);

        eventService.deleteEvent(eventId, username);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // =============================================
    // 이벤트 공유
    // =============================================

    /**
     * 이벤트 공유 목록 조회
     */
    @GetMapping("/{id}/shares")
    public ResponseEntity<ApiResponse<List<EventShareResponse>>> getShares(@PathVariable("id") Long eventId) {
        String username = SecurityUtils.getCurrentUsername();
        log.debug("Get event shares: eventId={}, username={}", eventId, username);

        List<EventShareResponse> shares = eventService.getShares(eventId, username);
        return ResponseEntity.ok(ApiResponse.success(shares));
    }

    /**
     * 이벤트 공유 추가
     */
    @PostMapping("/{id}/shares")
    public ResponseEntity<ApiResponse<Void>> addShare(
            @PathVariable("id") Long eventId,
            @Valid @RequestBody EventShareRequest request
    ) {
        String username = SecurityUtils.getCurrentUsername();
        log.debug("Add event share: eventId={}, targetUser={}", eventId, request.getUsername());

        eventService.addShare(eventId, request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(null));
    }

    /**
     * 이벤트 공유 해제
     */
    @DeleteMapping("/{id}/shares/{targetUsername}")
    public ResponseEntity<ApiResponse<Void>> removeShare(
            @PathVariable("id") Long eventId,
            @PathVariable("targetUsername") String targetUsername
    ) {
        String username = SecurityUtils.getCurrentUsername();
        log.debug("Remove event share: eventId={}, targetUser={}", eventId, targetUsername);

        eventService.removeShare(eventId, targetUsername, username);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 이벤트 공유 권한 변경
     */
    @PutMapping("/{id}/shares/{targetUsername}")
    public ResponseEntity<ApiResponse<Void>> updateShareType(
            @PathVariable("id") Long eventId,
            @PathVariable("targetUsername") String targetUsername,
            @RequestParam("shareType") String shareType
    ) {
        String username = SecurityUtils.getCurrentUsername();
        log.debug("Update event share type: eventId={}, targetUser={}, shareType={}",
                eventId, targetUsername, shareType);

        eventService.updateShareType(eventId, targetUsername, shareType, username);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // =============================================
    // 이벤트 이관
    // =============================================

    /**
     * 이벤트 이관
     */
    @PostMapping("/{id}/transfer")
    public ResponseEntity<ApiResponse<EventDetailResponse>> transferEvent(
            @PathVariable("id") Long eventId,
            @Valid @RequestBody EventTransferRequest request
    ) {
        String username = SecurityUtils.getCurrentUsername();
        log.debug("Transfer event: eventId={}, targetUser={}", eventId, request.getTargetUsername());

        EventDetailResponse event = eventService.transferEvent(eventId, request, username);
        return ResponseEntity.ok(ApiResponse.success(event));
    }
}
