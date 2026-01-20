package com.taskflow.domain;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 이벤트 도메인
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    // 상태 상수
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_DELETED = "DELETED";

    // 반복 유형 상수
    public static final String RECURRENCE_DAILY = "DAILY";
    public static final String RECURRENCE_WEEKLY = "WEEKLY";
    public static final String RECURRENCE_MONTHLY = "MONTHLY";
    public static final String RECURRENCE_YEARLY = "YEARLY";
    public static final String RECURRENCE_MONTHLY_LUNAR = "MONTHLY_LUNAR";
    public static final String RECURRENCE_YEARLY_LUNAR = "YEARLY_LUNAR";

    private Long eventId;
    private Long calendarId;
    private String ownerUsername;
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

    // 반복 관련
    private Boolean isRecurring;
    private String recurrenceType;
    private Integer recurrenceInterval;
    private LocalDate recurrenceEndDate;
    private Integer recurrenceCount;
    private String recurrenceDays;
    private String exceptionDates;
    private Long parentEventId;

    // 그룹
    private Long groupId;

    // 상태
    private String status;

    // 감사 필드
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;

    // 조인 필드 (조회용)
    private String ownerName;           // 소유자 이름
    private String calendarName;        // 캘린더 이름
    private String calendarColor;       // 캘린더 색상
    private Boolean isShared;           // 공유받은 이벤트 여부
    private String sharedByUsername;    // 공유해준 사용자
    private String sharedByName;        // 공유해준 사용자 이름
    private String shareType;           // 공유 타입 (VIEW/EDIT)
    private String accessType;          // 접근 타입 (OWNER/VIEW/EDIT)
    private String groupName;           // 그룹명
    private String groupColor;          // 그룹 색상
    private Boolean isGroupShared;      // 그룹 공유 여부 (본인 소유가 아닌 그룹 이벤트)

    // Lombok getter 명시 (특수 명명 패턴)
    public Boolean getIsDefault() {
        return isAllDay;
    }

    public Boolean getIsLunar() {
        return isLunar;
    }

    public Boolean getIsRecurring() {
        return isRecurring;
    }

    public Boolean getIsShared() {
        return isShared;
    }

    public Boolean getIsAllDay() {
        return isAllDay;
    }

    public Boolean getLunarLeapMonth() {
        return lunarLeapMonth;
    }

    public Long getGroupId() {
        return groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getGroupColor() {
        return groupColor;
    }

    public Boolean getIsGroupShared() {
        return isGroupShared;
    }
}
