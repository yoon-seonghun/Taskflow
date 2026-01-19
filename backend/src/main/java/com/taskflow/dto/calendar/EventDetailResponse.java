package com.taskflow.dto.calendar;

import com.taskflow.domain.Event;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 이벤트 상세 응답 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventDetailResponse {

    private Long eventId;
    private Long calendarId;
    private String calendarName;
    private String calendarColor;
    private String ownerUsername;
    private String ownerName;
    private String title;
    private String description;
    private String location;

    // 날짜/시간
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Boolean isAllDay;

    // 음력 관련
    private Boolean isLunar;
    private Integer lunarMonth;
    private Integer lunarDay;
    private Boolean lunarLeapMonth;
    private String lunarDisplayText;    // "12.5" 또는 "윤4.1"

    // 반복 관련
    private Boolean isRecurring;
    private String recurrenceType;
    private Integer recurrenceInterval;
    private LocalDate recurrenceEndDate;
    private Integer recurrenceCount;
    private String recurrenceDays;
    private Long parentEventId;

    // 상태
    private String status;

    // 공유 정보
    private Boolean isShared;
    private String sharedByUsername;
    private String sharedByName;
    private String shareType;
    private String accessType;          // OWNER, VIEW, EDIT

    // 감사 정보
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;

    public static EventDetailResponse from(Event event) {
        return EventDetailResponse.builder()
                .eventId(event.getEventId())
                .calendarId(event.getCalendarId())
                .calendarName(event.getCalendarName())
                .calendarColor(event.getCalendarColor())
                .ownerUsername(event.getOwnerUsername())
                .ownerName(event.getOwnerName())
                .title(event.getTitle())
                .description(event.getDescription())
                .location(event.getLocation())
                .startDate(event.getStartDate())
                .endDate(event.getEndDate())
                .startTime(event.getStartTime())
                .endTime(event.getEndTime())
                .isAllDay(event.getIsAllDay())
                .isLunar(event.getIsLunar())
                .lunarMonth(event.getLunarMonth())
                .lunarDay(event.getLunarDay())
                .lunarLeapMonth(event.getLunarLeapMonth())
                .lunarDisplayText(formatLunarDate(event))
                .isRecurring(event.getIsRecurring())
                .recurrenceType(event.getRecurrenceType())
                .recurrenceInterval(event.getRecurrenceInterval())
                .recurrenceEndDate(event.getRecurrenceEndDate())
                .recurrenceCount(event.getRecurrenceCount())
                .recurrenceDays(event.getRecurrenceDays())
                .parentEventId(event.getParentEventId())
                .status(event.getStatus())
                .isShared(event.getIsShared())
                .sharedByUsername(event.getSharedByUsername())
                .sharedByName(event.getSharedByName())
                .shareType(event.getShareType())
                .accessType(event.getAccessType())
                .createdAt(event.getCreatedAt())
                .createdBy(event.getCreatedBy())
                .updatedAt(event.getUpdatedAt())
                .updatedBy(event.getUpdatedBy())
                .build();
    }

    public static List<EventDetailResponse> fromList(List<Event> events) {
        return events.stream()
                .map(EventDetailResponse::from)
                .collect(Collectors.toList());
    }

    private static String formatLunarDate(Event event) {
        if (event.getLunarMonth() == null || event.getLunarDay() == null) {
            return null;
        }
        String prefix = Boolean.TRUE.equals(event.getLunarLeapMonth()) ? "윤" : "";
        return prefix + event.getLunarMonth() + "." + event.getLunarDay();
    }
}
