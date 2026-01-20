package com.taskflow.dto.calendar;

import lombok.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 이벤트 생성/수정 요청 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRequest {

    @NotNull(message = "캘린더 ID는 필수입니다.")
    private Long calendarId;

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 200, message = "제목은 200자를 초과할 수 없습니다.")
    private String title;

    private String description;

    @Size(max = 200, message = "장소는 200자를 초과할 수 없습니다.")
    private String location;

    // 날짜/시간
    @NotNull(message = "시작일은 필수입니다.")
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
    private String recurrenceType;      // DAILY, WEEKLY, MONTHLY, YEARLY, MONTHLY_LUNAR, YEARLY_LUNAR
    private Integer recurrenceInterval;
    private LocalDate recurrenceEndDate;
    private Integer recurrenceCount;
    private String recurrenceDays;      // 주간 반복 요일: "0,1,2,3,4,5,6"

    // 그룹
    private Long groupId;

    // 기본값 처리
    public Boolean getIsAllDay() {
        return isAllDay != null ? isAllDay : true;
    }

    public Boolean getIsLunar() {
        return isLunar != null ? isLunar : false;
    }

    public Boolean getIsRecurring() {
        return isRecurring != null ? isRecurring : false;
    }

    public Boolean getLunarLeapMonth() {
        return lunarLeapMonth != null ? lunarLeapMonth : false;
    }

    public Integer getRecurrenceInterval() {
        return recurrenceInterval != null ? recurrenceInterval : 1;
    }
}
