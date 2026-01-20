package com.taskflow.dto.calendar;

import lombok.*;

import jakarta.validation.constraints.NotBlank;

/**
 * 캘린더 공유 요청 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalendarShareRequest {

    @NotBlank(message = "공유 대상 사용자는 필수입니다.")
    private String username;

    private String shareType;  // VIEW (기본), EDIT

    public String getShareType() {
        return shareType != null ? shareType : "VIEW";
    }
}
