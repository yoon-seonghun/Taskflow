package com.taskflow.service;

import com.taskflow.domain.Calendar;
import com.taskflow.domain.Event;
import com.taskflow.domain.EventShare;
import com.taskflow.dto.calendar.*;
import com.taskflow.exception.BusinessException;
import com.taskflow.mapper.EventMapper;
import com.taskflow.mapper.EventShareMapper;
import com.taskflow.mapper.UserCalendarMapper;
import com.taskflow.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 이벤트 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventMapper eventMapper;
    private final EventShareMapper shareMapper;
    private final UserCalendarMapper calendarMapper;
    private final UserMapper userMapper;
    private final UserCalendarService calendarService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final LunarCalendarService lunarService;

    // =============================================
    // 이벤트 CRUD
    // =============================================

    /**
     * 기간별 이벤트 조회 (반복 이벤트 인스턴스 포함)
     */
    @Transactional(readOnly = true)
    public List<EventDetailResponse> getEventsByDateRange(
            String username,
            LocalDate startDate,
            LocalDate endDate,
            Long calendarId,
            Boolean includeShared
    ) {
        List<Event> events = eventMapper.selectByDateRange(
                username, startDate, endDate, calendarId,
                includeShared != null ? includeShared : true
        );

        // JOIN으로 인한 중복 제거 (eventId 기준)
        Map<Long, Event> uniqueEvents = new LinkedHashMap<>();
        for (Event event : events) {
            if (!uniqueEvents.containsKey(event.getEventId())) {
                uniqueEvents.put(event.getEventId(), event);
            }
        }

        // 반복 이벤트 인스턴스 확장
        List<Event> expandedEvents = new ArrayList<>();
        for (Event event : uniqueEvents.values()) {
            if (Boolean.TRUE.equals(event.getIsRecurring())) {
                expandedEvents.addAll(expandRecurringEvent(event, startDate, endDate));
            } else {
                expandedEvents.add(event);
            }
        }

        return EventDetailResponse.fromList(expandedEvents);
    }

    /**
     * 반복 이벤트를 날짜 범위 내 인스턴스로 확장
     */
    private List<Event> expandRecurringEvent(Event event, LocalDate rangeStart, LocalDate rangeEnd) {
        List<Event> instances = new ArrayList<>();

        LocalDate eventStart = event.getStartDate();
        LocalDate eventEnd = event.getEndDate() != null ? event.getEndDate() : eventStart;
        long eventDuration = java.time.temporal.ChronoUnit.DAYS.between(eventStart, eventEnd);

        String recurrenceType = event.getRecurrenceType();
        int interval = event.getRecurrenceInterval() != null ? event.getRecurrenceInterval() : 1;
        LocalDate recurrenceEnd = event.getRecurrenceEndDate();
        Integer maxCount = event.getRecurrenceCount();

        // 반복 종료일 결정 (recurrenceEndDate, recurrenceCount, 또는 rangeEnd 중 가장 작은 값)
        LocalDate effectiveEnd = rangeEnd;
        if (recurrenceEnd != null && recurrenceEnd.isBefore(effectiveEnd)) {
            effectiveEnd = recurrenceEnd;
        }

        LocalDate currentDate = eventStart;
        int count = 0;
        int maxIterations = 1000; // 무한 루프 방지
        int iteration = 0;

        while (!currentDate.isAfter(effectiveEnd) && iteration < maxIterations) {
            iteration++;

            // 최대 반복 횟수 확인
            if (maxCount != null && count >= maxCount) {
                break;
            }

            // 현재 날짜가 조회 범위 내에 있으면 인스턴스 추가
            LocalDate instanceEnd = currentDate.plusDays(eventDuration);
            if (!currentDate.isAfter(rangeEnd) && !instanceEnd.isBefore(rangeStart)) {
                Event instance = cloneEventWithDate(event, currentDate, instanceEnd);
                instances.add(instance);
            }

            // 다음 반복 날짜 계산
            currentDate = calculateNextOccurrence(currentDate, recurrenceType, interval, event);
            count++;

            // 날짜 계산 실패 시 중단
            if (currentDate == null) {
                break;
            }
        }

        return instances;
    }

    /**
     * 다음 반복 발생일 계산
     */
    private LocalDate calculateNextOccurrence(LocalDate current, String recurrenceType, int interval, Event event) {
        if (recurrenceType == null) {
            return null;
        }

        switch (recurrenceType) {
            case "DAILY":
                return current.plusDays(interval);

            case "WEEKLY":
                return current.plusWeeks(interval);

            case "MONTHLY":
                return current.plusMonths(interval);

            case "YEARLY":
                return current.plusYears(interval);

            // 음력 반복
            case "WEEKLY_LUNAR":
                return calculateNextLunarWeekDate(current, interval, event);

            case "MONTHLY_LUNAR":
                return calculateNextLunarMonthDate(current, interval, event);

            case "YEARLY_LUNAR":
                return calculateNextLunarYearDate(current, interval, event);

            default:
                return current.plusDays(interval);
        }
    }

    /**
     * 음력 주간 반복 - 다음 발생일 계산
     * 음력 기준 7일(interval*7일) 후의 양력 날짜 반환
     */
    private LocalDate calculateNextLunarWeekDate(LocalDate current, int interval, Event event) {
        try {
            // 현재 양력 → 음력 변환
            LunarConversionResponse currentLunar = lunarService.solarToLunar(current);
            LunarDateResponse lunar = currentLunar.getLunar();

            // 음력 기준 7*interval일 후 계산
            int targetDay = lunar.getDay() + (7 * interval);
            int targetMonth = lunar.getMonth();
            int targetYear = lunar.getYear();
            boolean isLeapMonth = Boolean.TRUE.equals(lunar.getIsLeapMonth());

            // 월/연도 넘김 처리
            while (targetDay > getLunarMonthDaysForCalc(targetYear, targetMonth, isLeapMonth)) {
                targetDay -= getLunarMonthDaysForCalc(targetYear, targetMonth, isLeapMonth);

                // 윤달 처리
                if (!isLeapMonth && lunarService.getLeapMonth(targetYear) == targetMonth) {
                    isLeapMonth = true;
                } else {
                    isLeapMonth = false;
                    targetMonth++;
                    if (targetMonth > 12) {
                        targetMonth = 1;
                        targetYear++;
                    }
                }
            }

            // 음력 → 양력 변환
            LunarConversionResponse result = lunarService.lunarToSolar(targetYear, targetMonth, targetDay, isLeapMonth);
            return LocalDate.parse(result.getSolar());
        } catch (Exception e) {
            log.warn("Failed to calculate next lunar week date: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 음력 월간 반복 - 다음 발생일 계산
     * 매월 같은 음력 일자의 양력 날짜 반환
     */
    private LocalDate calculateNextLunarMonthDate(LocalDate current, int interval, Event event) {
        try {
            // 원본 이벤트의 음력 정보 사용
            Integer lunarDay = event.getLunarDay();
            Boolean isLeapMonth = event.getLunarLeapMonth();

            if (lunarDay == null) {
                log.warn("Lunar day is null for event: {}", event.getEventId());
                return null;
            }

            // 현재 날짜의 음력 정보
            LunarConversionResponse currentLunar = lunarService.solarToLunar(current);
            int currentLunarMonth = currentLunar.getLunar().getMonth();
            int currentLunarYear = currentLunar.getLunar().getYear();

            // 다음 음력 월 계산
            int targetMonth = currentLunarMonth + interval;
            int targetYear = currentLunarYear;

            while (targetMonth > 12) {
                targetMonth -= 12;
                targetYear++;
            }

            // 윤달 처리: 해당 년도/월에 윤달이 없으면 평달로
            boolean targetLeapMonth = Boolean.TRUE.equals(isLeapMonth)
                    && lunarService.hasLeapMonth(targetYear, targetMonth);

            // 해당 월의 최대 일수 확인
            int maxDays = getLunarMonthDaysForCalc(targetYear, targetMonth, targetLeapMonth);
            int targetDay = Math.min(lunarDay, maxDays);

            // 윤달 대체 시 설명 업데이트
            if (Boolean.TRUE.equals(isLeapMonth) && !targetLeapMonth) {
                appendLeapMonthNote(event, targetYear, targetMonth);
            }

            // 음력 → 양력 변환
            LunarConversionResponse result = lunarService.lunarToSolar(targetYear, targetMonth, targetDay, targetLeapMonth);
            return LocalDate.parse(result.getSolar());
        } catch (Exception e) {
            log.warn("Failed to calculate next lunar month date: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 음력 연간 반복 - 다음 발생일 계산
     * 매년 같은 음력 월/일의 양력 날짜 반환
     */
    private LocalDate calculateNextLunarYearDate(LocalDate current, int interval, Event event) {
        try {
            // 원본 이벤트의 음력 정보 사용
            Integer lunarMonth = event.getLunarMonth();
            Integer lunarDay = event.getLunarDay();
            Boolean isLeapMonth = event.getLunarLeapMonth();

            if (lunarMonth == null || lunarDay == null) {
                log.warn("Lunar month/day is null for event: {}", event.getEventId());
                return null;
            }

            // 현재 날짜의 음력 연도
            LunarConversionResponse currentLunar = lunarService.solarToLunar(current);
            int targetYear = currentLunar.getLunar().getYear() + interval;

            // 윤달 처리
            boolean targetLeapMonth = Boolean.TRUE.equals(isLeapMonth)
                    && lunarService.hasLeapMonth(targetYear, lunarMonth);

            // 해당 월의 최대 일수 확인
            int maxDays = getLunarMonthDaysForCalc(targetYear, lunarMonth, targetLeapMonth);
            int targetDay = Math.min(lunarDay, maxDays);

            // 윤달 대체 시 설명 업데이트
            if (Boolean.TRUE.equals(isLeapMonth) && !targetLeapMonth) {
                appendLeapMonthNote(event, targetYear, lunarMonth);
            }

            // 음력 → 양력 변환
            LunarConversionResponse result = lunarService.lunarToSolar(targetYear, lunarMonth, targetDay, targetLeapMonth);
            return LocalDate.parse(result.getSolar());
        } catch (Exception e) {
            log.warn("Failed to calculate next lunar year date: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 음력 월의 일수 조회 (윤달 여부 고려)
     */
    private int getLunarMonthDaysForCalc(int year, int month, boolean isLeapMonth) {
        if (isLeapMonth) {
            return lunarService.getLeapMonthDays(year);
        }
        return lunarService.getLunarMonthDays(year, month);
    }

    /**
     * 윤달 대체 시 이벤트 설명에 기록
     */
    private void appendLeapMonthNote(Event event, int year, int month) {
        String note = String.format("[%d년 %d월: 윤달 없음 → 평달로 대체]", year, month);
        String currentDesc = event.getDescription();
        if (currentDesc == null || currentDesc.isEmpty()) {
            event.setDescription(note);
        } else if (!currentDesc.contains(note)) {
            event.setDescription(currentDesc + "\n" + note);
        }
    }

    /**
     * 이벤트 복제 (날짜만 변경)
     */
    private Event cloneEventWithDate(Event original, LocalDate newStartDate, LocalDate newEndDate) {
        return Event.builder()
                .eventId(original.getEventId())
                .calendarId(original.getCalendarId())
                .ownerUsername(original.getOwnerUsername())
                .title(original.getTitle())
                .description(original.getDescription())
                .location(original.getLocation())
                .startDate(newStartDate)
                .endDate(newEndDate)
                .startTime(original.getStartTime())
                .endTime(original.getEndTime())
                .isAllDay(original.getIsAllDay())
                .isLunar(original.getIsLunar())
                .lunarMonth(original.getLunarMonth())
                .lunarDay(original.getLunarDay())
                .lunarLeapMonth(original.getLunarLeapMonth())
                .isRecurring(original.getIsRecurring())
                .recurrenceType(original.getRecurrenceType())
                .recurrenceInterval(original.getRecurrenceInterval())
                .recurrenceEndDate(original.getRecurrenceEndDate())
                .recurrenceCount(original.getRecurrenceCount())
                .recurrenceDays(original.getRecurrenceDays())
                .exceptionDates(original.getExceptionDates())
                .parentEventId(original.getParentEventId())
                .status(original.getStatus())
                .createdAt(original.getCreatedAt())
                .createdBy(original.getCreatedBy())
                .updatedAt(original.getUpdatedAt())
                .updatedBy(original.getUpdatedBy())
                // 조인 필드
                .ownerName(original.getOwnerName())
                .calendarName(original.getCalendarName())
                .calendarColor(original.getCalendarColor())
                .isShared(original.getIsShared())
                .sharedByUsername(original.getSharedByUsername())
                .sharedByName(original.getSharedByName())
                .shareType(original.getShareType())
                .accessType(original.getAccessType())
                .build();
    }

    /**
     * 이벤트 상세 조회
     */
    @Transactional(readOnly = true)
    public EventDetailResponse getEvent(Long eventId, String username) {
        Event event = eventMapper.findByIdWithShare(eventId, username)
                .orElseThrow(() -> new BusinessException("이벤트를 찾을 수 없거나 접근 권한이 없습니다."));
        return EventDetailResponse.from(event);
    }

    /**
     * 이벤트 생성
     */
    @Transactional
    public EventDetailResponse createEvent(EventRequest request, String username) {
        // 캘린더 접근 권한 확인
        String calendarPermission = calendarMapper.getPermission(request.getCalendarId(), username);
        if (calendarPermission == null) {
            throw new BusinessException("캘린더에 접근할 수 없습니다.");
        }
        if ("VIEW".equals(calendarPermission)) {
            throw new BusinessException("캘린더에 이벤트를 생성할 권한이 없습니다.");
        }

        // 종료일 기본값 (시작일과 같음)
        LocalDate endDate = request.getEndDate() != null ? request.getEndDate() : request.getStartDate();

        // 반복 종료일 유효성 검증
        if (Boolean.TRUE.equals(request.getIsRecurring()) && request.getRecurrenceEndDate() != null) {
            if (request.getRecurrenceEndDate().isBefore(endDate)) {
                throw new BusinessException("반복 종료일은 이벤트 종료일 이후여야 합니다.");
            }
        }

        Event event = Event.builder()
                .calendarId(request.getCalendarId())
                .ownerUsername(username)
                .title(request.getTitle())
                .description(request.getDescription())
                .location(request.getLocation())
                .startDate(request.getStartDate())
                .endDate(endDate)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .isAllDay(request.getIsAllDay())
                .isLunar(request.getIsLunar())
                .lunarMonth(request.getLunarMonth())
                .lunarDay(request.getLunarDay())
                .lunarLeapMonth(request.getLunarLeapMonth())
                .isRecurring(request.getIsRecurring())
                .recurrenceType(request.getRecurrenceType())
                .recurrenceInterval(request.getRecurrenceInterval())
                .recurrenceEndDate(request.getRecurrenceEndDate())
                .recurrenceCount(request.getRecurrenceCount())
                .recurrenceDays(request.getRecurrenceDays())
                .status(Event.STATUS_ACTIVE)
                .createdBy(username)
                .build();

        eventMapper.insert(event);

        log.info("Event created: id={}, title={}, owner={}", event.getEventId(), event.getTitle(), username);

        // 감사 로그
        auditLogService.log("EVENT", event.getEventId(), "CREATE", username,
                "이벤트 생성: " + event.getTitle(), null, null, null);

        return getEvent(event.getEventId(), username);
    }

    /**
     * 이벤트 수정
     */
    @Transactional
    public EventDetailResponse updateEvent(Long eventId, EventRequest request, String username) {
        Event existing = eventMapper.findByIdWithShare(eventId, username)
                .orElseThrow(() -> new BusinessException("이벤트를 찾을 수 없거나 접근 권한이 없습니다."));

        // 수정 권한 확인
        String permission = existing.getAccessType();
        if (permission == null || "VIEW".equals(permission)) {
            throw new BusinessException("이벤트를 수정할 권한이 없습니다.");
        }

        // 캘린더 변경 시 권한 확인
        if (!existing.getCalendarId().equals(request.getCalendarId())) {
            String newCalendarPermission = calendarMapper.getPermission(request.getCalendarId(), username);
            if (newCalendarPermission == null || "VIEW".equals(newCalendarPermission)) {
                throw new BusinessException("대상 캘린더에 이벤트를 이동할 권한이 없습니다.");
            }
        }

        // 반복 종료일 유효성 검증
        LocalDate eventEndDate = request.getEndDate() != null ? request.getEndDate() : request.getStartDate();
        if (Boolean.TRUE.equals(request.getIsRecurring()) && request.getRecurrenceEndDate() != null) {
            if (request.getRecurrenceEndDate().isBefore(eventEndDate)) {
                throw new BusinessException("반복 종료일은 이벤트 종료일 이후여야 합니다.");
            }
        }

        existing.setCalendarId(request.getCalendarId());
        existing.setTitle(request.getTitle());
        existing.setDescription(request.getDescription());
        existing.setLocation(request.getLocation());
        existing.setStartDate(request.getStartDate());
        existing.setEndDate(request.getEndDate() != null ? request.getEndDate() : request.getStartDate());
        existing.setStartTime(request.getStartTime());
        existing.setEndTime(request.getEndTime());
        existing.setIsAllDay(request.getIsAllDay());
        existing.setIsLunar(request.getIsLunar());
        existing.setLunarMonth(request.getLunarMonth());
        existing.setLunarDay(request.getLunarDay());
        existing.setLunarLeapMonth(request.getLunarLeapMonth());
        existing.setIsRecurring(request.getIsRecurring());
        existing.setRecurrenceType(request.getRecurrenceType());
        existing.setRecurrenceInterval(request.getRecurrenceInterval());
        existing.setRecurrenceEndDate(request.getRecurrenceEndDate());
        existing.setRecurrenceCount(request.getRecurrenceCount());
        existing.setRecurrenceDays(request.getRecurrenceDays());
        existing.setUpdatedBy(username);

        eventMapper.update(existing);

        log.info("Event updated: id={}, title={}", eventId, existing.getTitle());

        return getEvent(eventId, username);
    }

    /**
     * 이벤트 삭제
     */
    @Transactional
    public void deleteEvent(Long eventId, String username) {
        Event event = eventMapper.findByIdWithShare(eventId, username)
                .orElseThrow(() -> new BusinessException("이벤트를 찾을 수 없거나 접근 권한이 없습니다."));

        // 삭제 권한 확인 (소유자만)
        if (!"OWNER".equals(event.getAccessType())) {
            throw new BusinessException("이벤트를 삭제할 권한이 없습니다.");
        }

        // 공유 모두 해제
        shareMapper.deleteAllByEventId(eventId, username);

        // 이벤트 삭제 (상태 변경)
        eventMapper.delete(eventId, username);

        log.info("Event deleted: id={}", eventId);

        // 감사 로그
        auditLogService.log("EVENT", eventId, "DELETE", username,
                "이벤트 삭제: " + event.getTitle(), null, null, null);
    }

    // =============================================
    // 이벤트 공유
    // =============================================

    /**
     * 이벤트 공유 목록 조회
     */
    @Transactional(readOnly = true)
    public List<EventShareResponse> getShares(Long eventId, String username) {
        Event event = eventMapper.findById(eventId)
                .orElseThrow(() -> new BusinessException("이벤트를 찾을 수 없습니다."));

        // 소유자만 공유 목록 조회 가능
        if (!event.getOwnerUsername().equals(username)) {
            throw new BusinessException("공유 목록을 조회할 권한이 없습니다.");
        }

        List<EventShare> shares = shareMapper.selectByEventId(eventId);
        return EventShareResponse.fromList(shares);
    }

    /**
     * 이벤트 공유 추가
     */
    @Transactional
    public void addShare(Long eventId, EventShareRequest request, String username) {
        Event event = eventMapper.findById(eventId)
                .orElseThrow(() -> new BusinessException("이벤트를 찾을 수 없습니다."));

        // 소유자만 공유 가능
        if (!event.getOwnerUsername().equals(username)) {
            throw new BusinessException("이벤트를 공유할 권한이 없습니다.");
        }

        // 자기 자신에게 공유 불가
        if (request.getUsername().equals(username)) {
            throw new BusinessException("자기 자신에게는 공유할 수 없습니다.");
        }

        // 대상 사용자 존재 확인
        if (!userMapper.existsByUsername(request.getUsername())) {
            throw new BusinessException("공유 대상 사용자를 찾을 수 없습니다.");
        }

        // 중복 공유 확인
        if (shareMapper.exists(eventId, request.getUsername())) {
            throw new BusinessException("이미 공유된 사용자입니다.");
        }

        EventShare share = EventShare.builder()
                .eventId(eventId)
                .shareType(request.getShareType())
                .username(request.getUsername())
                .sharedBy(username)
                .createdBy(username)
                .build();

        shareMapper.insert(share);

        log.info("Event {} shared with user {} (type: {})", eventId, request.getUsername(), request.getShareType());

        // 알림 발송 (TODO: NotificationService에 메서드 추가 필요)
        // notificationService.sendEventSharedNotification(eventId, event.getTitle(), request.getUsername(), username);

        // 감사 로그
        auditLogService.log("EVENT_SHARE", eventId, "SHARE", username,
                String.format("이벤트 공유: %s → %s (%s)", event.getTitle(), request.getUsername(), request.getShareType()),
                null, null, request.getUsername());
    }

    /**
     * 이벤트 공유 해제
     */
    @Transactional
    public void removeShare(Long eventId, String targetUsername, String username) {
        Event event = eventMapper.findById(eventId)
                .orElseThrow(() -> new BusinessException("이벤트를 찾을 수 없습니다."));

        // 소유자만 공유 해제 가능
        if (!event.getOwnerUsername().equals(username)) {
            throw new BusinessException("공유를 해제할 권한이 없습니다.");
        }

        // 공유 존재 확인
        if (!shareMapper.exists(eventId, targetUsername)) {
            throw new BusinessException("공유 정보를 찾을 수 없습니다.");
        }

        shareMapper.delete(eventId, targetUsername, username);

        log.info("Event {} share removed for user {}", eventId, targetUsername);

        // 감사 로그
        auditLogService.log("EVENT_SHARE", eventId, "UNSHARE", username,
                String.format("이벤트 공유 해제: %s → %s", event.getTitle(), targetUsername),
                null, null, targetUsername);
    }

    // =============================================
    // 이벤트 이관
    // =============================================

    /**
     * 이벤트 이관
     */
    @Transactional
    public EventDetailResponse transferEvent(Long eventId, EventTransferRequest request, String username) {
        Event event = eventMapper.findById(eventId)
                .orElseThrow(() -> new BusinessException("이벤트를 찾을 수 없습니다."));

        // 소유자만 이관 가능
        if (!event.getOwnerUsername().equals(username)) {
            throw new BusinessException("이벤트를 이관할 권한이 없습니다.");
        }

        // 자기 자신에게 이관 불가
        if (request.getTargetUsername().equals(username)) {
            throw new BusinessException("자기 자신에게는 이관할 수 없습니다.");
        }

        // 대상 사용자 존재 확인
        if (!userMapper.existsByUsername(request.getTargetUsername())) {
            throw new BusinessException("이관 대상 사용자를 찾을 수 없습니다.");
        }

        // 대상 사용자의 기본 캘린더 조회 또는 생성
        Calendar targetCalendar = calendarService.getOrCreateDefaultCalendar(request.getTargetUsername());

        // 기존 공유 모두 해제
        shareMapper.deleteAllByEventId(eventId, username);

        // 이관 이력 기록
        EventShare transferHistory = EventShare.builder()
                .eventId(eventId)
                .shareType(EventShare.TYPE_TRANSFER)
                .username(request.getTargetUsername())
                .sharedBy(username)
                .createdBy(username)
                .build();
        shareMapper.insertTransferHistory(transferHistory);

        // 소유자 및 캘린더 변경
        eventMapper.transferOwner(eventId, request.getTargetUsername(), targetCalendar.getCalendarId(), username);

        log.info("Event {} transferred from {} to {}", eventId, username, request.getTargetUsername());

        // 알림 발송 (TODO: NotificationService에 메서드 추가 필요)
        // notificationService.sendEventTransferredNotification(eventId, event.getTitle(), request.getTargetUsername(), username);

        // 감사 로그
        auditLogService.log("EVENT", eventId, "TRANSFER", username,
                String.format("이벤트 이관: %s → %s", event.getTitle(), request.getTargetUsername()),
                null, null, request.getTargetUsername());

        return getEvent(eventId, request.getTargetUsername());
    }

    // =============================================
    // 권한 확인
    // =============================================

    /**
     * 이벤트 접근 권한 확인
     */
    @Transactional(readOnly = true)
    public boolean hasAccess(Long eventId, String username) {
        return eventMapper.hasAccess(eventId, username);
    }

    /**
     * 이벤트 권한 조회
     */
    @Transactional(readOnly = true)
    public String getPermission(Long eventId, String username) {
        return eventMapper.getPermission(eventId, username);
    }

    /**
     * 이벤트 수정 권한 확인
     */
    @Transactional(readOnly = true)
    public boolean canEdit(Long eventId, String username) {
        String permission = getPermission(eventId, username);
        return "OWNER".equals(permission) || "EDIT".equals(permission);
    }
}
