package com.taskflow.dto.calendar;

import lombok.*;

import jakarta.validation.constraints.NotBlank;

/**
 * 이벤트 이관 요청 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventTransferRequest {

    @NotBlank(message = "이관 대상 사용자는 필수입니다.")
    private String targetUsername;
}
