package com.taskflow.dto.calendar;

import com.taskflow.domain.CalendarShare;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 캘린더 공유 응답 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarShareResponse {

    private Long calendarShareId;
    private Long calendarId;
    private String calendarName;
    private String shareType;
    private String username;
    private String userName;
    private String departmentName;
    private String sharedBy;
    private String sharedByName;
    private LocalDateTime sharedAt;

    public static CalendarShareResponse from(CalendarShare share) {
        return CalendarShareResponse.builder()
                .calendarShareId(share.getCalendarShareId())
                .calendarId(share.getCalendarId())
                .calendarName(share.getCalendarName())
                .shareType(share.getShareType())
                .username(share.getUsername())
                .userName(share.getUserName())
                .departmentName(share.getDepartmentName())
                .sharedBy(share.getSharedBy())
                .sharedByName(share.getSharedByName())
                .sharedAt(share.getSharedAt())
                .build();
    }

    public static List<CalendarShareResponse> fromList(List<CalendarShare> shares) {
        return shares.stream()
                .map(CalendarShareResponse::from)
                .collect(Collectors.toList());
    }
}
