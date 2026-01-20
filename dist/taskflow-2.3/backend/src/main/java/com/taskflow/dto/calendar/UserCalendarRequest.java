package com.taskflow.dto.calendar;

import lombok.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 사용자 캘린더 생성/수정 요청 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCalendarRequest {

    @NotBlank(message = "캘린더 이름은 필수입니다.")
    @Size(max = 100, message = "캘린더 이름은 100자를 초과할 수 없습니다.")
    private String calendarName;

    @Size(max = 500, message = "설명은 500자를 초과할 수 없습니다.")
    private String description;

    @Size(max = 20, message = "색상 코드는 20자를 초과할 수 없습니다.")
    private String color;

    private Boolean isDefault;

    private Integer sortOrder;
}
