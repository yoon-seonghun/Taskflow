package com.taskflow.dto.calendar;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.taskflow.domain.Calendar;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 사용자 캘린더 응답 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCalendarResponse {

    private Long calendarId;
    private String ownerUsername;
    private String ownerName;
    private String calendarName;
    private String description;
    private String color;

    @JsonProperty("isDefault")
    private Boolean isDefault;

    private Integer sortOrder;
    private String useYn;

    // 이벤트 수
    private Integer eventCount;

    // 공유 정보 (공유받은 캘린더인 경우)
    @JsonProperty("isShared")
    private Boolean isShared;

    private String sharedByUsername;
    private String sharedByName;
    private String shareType;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static UserCalendarResponse from(Calendar calendar) {
        return UserCalendarResponse.builder()
                .calendarId(calendar.getCalendarId())
                .ownerUsername(calendar.getOwnerUsername())
                .ownerName(calendar.getOwnerName())
                .calendarName(calendar.getCalendarName())
                .description(calendar.getDescription())
                .color(calendar.getColor())
                .isDefault(calendar.getIsDefault())
                .sortOrder(calendar.getSortOrder())
                .useYn(calendar.getUseYn())
                .eventCount(calendar.getEventCount())
                .isShared(calendar.getIsShared())
                .sharedByUsername(calendar.getSharedByUsername())
                .sharedByName(calendar.getSharedByName())
                .shareType(calendar.getShareType())
                .createdAt(calendar.getCreatedAt())
                .updatedAt(calendar.getUpdatedAt())
                .build();
    }

    public static List<UserCalendarResponse> fromList(List<Calendar> calendars) {
        return calendars.stream()
                .map(UserCalendarResponse::from)
                .collect(Collectors.toList());
    }
}
