package com.taskflow.service;

import com.taskflow.domain.Calendar;
import com.taskflow.domain.CalendarShare;
import com.taskflow.dto.calendar.*;
import com.taskflow.exception.BusinessException;
import com.taskflow.mapper.CalendarShareMapper;
import com.taskflow.mapper.EventMapper;
import com.taskflow.mapper.UserCalendarMapper;
import com.taskflow.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 사용자 캘린더 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCalendarService {

    private final UserCalendarMapper calendarMapper;
    private final CalendarShareMapper shareMapper;
    private final EventMapper eventMapper;
    private final UserMapper userMapper;
    private final AuditLogService auditLogService;

    // =============================================
    // 캘린더 CRUD
    // =============================================

    /**
     * 내 캘린더 목록 조회 (공유받은 캘린더 포함)
     */
    @Transactional(readOnly = true)
    public List<UserCalendarResponse> getMyCalendars(String username) {
        List<Calendar> calendars = calendarMapper.selectMyCalendars(username);
        return UserCalendarResponse.fromList(calendars);
    }

    /**
     * 캘린더 상세 조회
     */
    @Transactional(readOnly = true)
    public UserCalendarResponse getCalendar(Long calendarId, String username) {
        Calendar calendar = calendarMapper.findByIdWithShare(calendarId, username)
                .orElseThrow(() -> new BusinessException("캘린더를 찾을 수 없습니다."));
        return UserCalendarResponse.from(calendar);
    }

    /**
     * 캘린더 생성
     */
    @Transactional
    public UserCalendarResponse createCalendar(UserCalendarRequest request, String username) {
        // 기본 캘린더 여부 확인
        boolean isDefault = Boolean.TRUE.equals(request.getIsDefault());

        // 기본 캘린더로 설정하면 기존 기본 캘린더 해제
        if (isDefault) {
            calendarMapper.clearDefaultCalendar(username);
        }

        // 정렬 순서
        Integer maxOrder = calendarMapper.getMaxSortOrder(username);
        int sortOrder = request.getSortOrder() != null ? request.getSortOrder() : (maxOrder != null ? maxOrder + 1 : 0);

        Calendar calendar = Calendar.builder()
                .ownerUsername(username)
                .calendarName(request.getCalendarName())
                .description(request.getDescription())
                .color(request.getColor() != null ? request.getColor() : "#3b82f6")
                .isDefault(isDefault)
                .sortOrder(sortOrder)
                .useYn("Y")
                .createdBy(username)
                .build();

        calendarMapper.insert(calendar);

        log.info("Calendar created: id={}, name={}, owner={}", calendar.getCalendarId(), calendar.getCalendarName(), username);

        // 감사 로그
        auditLogService.log("CALENDAR", calendar.getCalendarId(), "CREATE", username,
                "캘린더 생성: " + calendar.getCalendarName(), null, null, null);

        return getCalendar(calendar.getCalendarId(), username);
    }

    /**
     * 캘린더 수정
     */
    @Transactional
    public UserCalendarResponse updateCalendar(Long calendarId, UserCalendarRequest request, String username) {
        Calendar existing = calendarMapper.findById(calendarId)
                .orElseThrow(() -> new BusinessException("캘린더를 찾을 수 없습니다."));

        // 소유자 확인
        if (!existing.getOwnerUsername().equals(username)) {
            throw new BusinessException("캘린더를 수정할 권한이 없습니다.");
        }

        // 기본 캘린더 변경 처리
        if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(existing.getIsDefault())) {
            calendarMapper.clearDefaultCalendar(username);
        }

        existing.setCalendarName(request.getCalendarName());
        existing.setDescription(request.getDescription());
        if (request.getColor() != null) {
            existing.setColor(request.getColor());
        }
        if (request.getIsDefault() != null) {
            existing.setIsDefault(request.getIsDefault());
        }
        if (request.getSortOrder() != null) {
            existing.setSortOrder(request.getSortOrder());
        }
        existing.setUpdatedBy(username);

        calendarMapper.update(existing);

        log.info("Calendar updated: id={}, name={}", calendarId, existing.getCalendarName());

        return getCalendar(calendarId, username);
    }

    /**
     * 캘린더 삭제
     */
    @Transactional
    public void deleteCalendar(Long calendarId, String username) {
        Calendar calendar = calendarMapper.findById(calendarId)
                .orElseThrow(() -> new BusinessException("캘린더를 찾을 수 없습니다."));

        // 소유자 확인
        if (!calendar.getOwnerUsername().equals(username)) {
            throw new BusinessException("캘린더를 삭제할 권한이 없습니다.");
        }

        // 기본 캘린더는 삭제 불가
        if (Boolean.TRUE.equals(calendar.getIsDefault())) {
            throw new BusinessException("기본 캘린더는 삭제할 수 없습니다.");
        }

        // 이벤트가 있는 경우 삭제 불가
        int eventCount = eventMapper.countByCalendarId(calendarId);
        if (eventCount > 0) {
            throw new BusinessException(String.format("캘린더에 %d개의 이벤트가 있습니다. 이벤트를 먼저 삭제하거나 이동해주세요.", eventCount));
        }

        // 공유 모두 해제
        shareMapper.deleteAllByCalendarId(calendarId, username);

        // 캘린더 삭제 (논리 삭제)
        calendarMapper.delete(calendarId, username);

        log.info("Calendar deleted: id={}", calendarId);

        // 감사 로그
        auditLogService.log("CALENDAR", calendarId, "DELETE", username,
                "캘린더 삭제: " + calendar.getCalendarName(), null, null, null);
    }

    /**
     * 기본 캘린더 조회 또는 생성
     */
    @Transactional
    public Calendar getOrCreateDefaultCalendar(String username) {
        return calendarMapper.findDefaultCalendar(username)
                .orElseGet(() -> {
                    log.info("Creating default calendar for user: {}", username);

                    Calendar defaultCalendar = Calendar.builder()
                            .ownerUsername(username)
                            .calendarName("기본")
                            .description("기본 캘린더")
                            .color("#3b82f6")
                            .isDefault(true)
                            .sortOrder(0)
                            .useYn("Y")
                            .createdBy(username)
                            .build();

                    calendarMapper.insert(defaultCalendar);

                    return defaultCalendar;
                });
    }

    // =============================================
    // 캘린더 공유
    // =============================================

    /**
     * 캘린더 공유 목록 조회
     */
    @Transactional(readOnly = true)
    public List<CalendarShareResponse> getShares(Long calendarId, String username) {
        // 권한 확인 (소유자만 공유 목록 조회 가능)
        Calendar calendar = calendarMapper.findById(calendarId)
                .orElseThrow(() -> new BusinessException("캘린더를 찾을 수 없습니다."));

        if (!calendar.getOwnerUsername().equals(username)) {
            throw new BusinessException("공유 목록을 조회할 권한이 없습니다.");
        }

        List<CalendarShare> shares = shareMapper.selectByCalendarId(calendarId);
        return CalendarShareResponse.fromList(shares);
    }

    /**
     * 캘린더 공유 추가
     */
    @Transactional
    public void addShare(Long calendarId, CalendarShareRequest request, String username) {
        Calendar calendar = calendarMapper.findById(calendarId)
                .orElseThrow(() -> new BusinessException("캘린더를 찾을 수 없습니다."));

        // 소유자만 공유 가능
        if (!calendar.getOwnerUsername().equals(username)) {
            throw new BusinessException("캘린더를 공유할 권한이 없습니다.");
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
        if (shareMapper.exists(calendarId, request.getUsername())) {
            throw new BusinessException("이미 공유된 사용자입니다.");
        }

        CalendarShare share = CalendarShare.builder()
                .calendarId(calendarId)
                .shareType(request.getShareType())
                .username(request.getUsername())
                .sharedBy(username)
                .createdBy(username)
                .build();

        shareMapper.insert(share);

        log.info("Calendar {} shared with user {} (type: {})", calendarId, request.getUsername(), request.getShareType());

        // 감사 로그
        auditLogService.log("CALENDAR_SHARE", calendarId, "SHARE", username,
                String.format("캘린더 공유: %s → %s (%s)", calendar.getCalendarName(), request.getUsername(), request.getShareType()),
                null, null, request.getUsername());
    }

    /**
     * 캘린더 공유 해제
     */
    @Transactional
    public void removeShare(Long calendarId, String targetUsername, String username) {
        Calendar calendar = calendarMapper.findById(calendarId)
                .orElseThrow(() -> new BusinessException("캘린더를 찾을 수 없습니다."));

        // 소유자만 공유 해제 가능
        if (!calendar.getOwnerUsername().equals(username)) {
            throw new BusinessException("공유를 해제할 권한이 없습니다.");
        }

        // 공유 존재 확인
        if (!shareMapper.exists(calendarId, targetUsername)) {
            throw new BusinessException("공유 정보를 찾을 수 없습니다.");
        }

        shareMapper.delete(calendarId, targetUsername, username);

        log.info("Calendar {} share removed for user {}", calendarId, targetUsername);

        // 감사 로그
        auditLogService.log("CALENDAR_SHARE", calendarId, "UNSHARE", username,
                String.format("캘린더 공유 해제: %s → %s", calendar.getCalendarName(), targetUsername),
                null, null, targetUsername);
    }

    /**
     * 캘린더 공유 타입 변경
     */
    @Transactional
    public void updateShareType(Long calendarId, String targetUsername, String shareType, String username) {
        Calendar calendar = calendarMapper.findById(calendarId)
                .orElseThrow(() -> new BusinessException("캘린더를 찾을 수 없습니다."));

        // 소유자만 변경 가능
        if (!calendar.getOwnerUsername().equals(username)) {
            throw new BusinessException("공유 권한을 변경할 권한이 없습니다.");
        }

        shareMapper.updateShareType(calendarId, targetUsername, shareType, username);

        log.info("Calendar {} share type updated for user {}: {}", calendarId, targetUsername, shareType);
    }

    // =============================================
    // 권한 확인
    // =============================================

    /**
     * 캘린더 접근 권한 확인
     */
    @Transactional(readOnly = true)
    public boolean hasAccess(Long calendarId, String username) {
        return calendarMapper.hasAccess(calendarId, username);
    }

    /**
     * 캘린더 권한 조회
     */
    @Transactional(readOnly = true)
    public String getPermission(Long calendarId, String username) {
        return calendarMapper.getPermission(calendarId, username);
    }

    /**
     * 캘린더 수정 권한 확인
     */
    @Transactional(readOnly = true)
    public boolean canEdit(Long calendarId, String username) {
        String permission = getPermission(calendarId, username);
        return "OWNER".equals(permission) || "EDIT".equals(permission);
    }
}
