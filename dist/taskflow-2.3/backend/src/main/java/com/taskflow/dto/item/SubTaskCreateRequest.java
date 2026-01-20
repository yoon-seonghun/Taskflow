package com.taskflow.dto.item;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.util.Map;

/**
 * 하위 업무 생성 요청 DTO (v2.2)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubTaskCreateRequest {

    /**
     * 업무 제목 (필수, 1~200자)
     */
    @NotBlank(message = "업무 제목은 필수입니다.")
    @Size(min = 1, max = 200, message = "업무 제목은 1~200자 사이여야 합니다.")
    private String title;

    /**
     * 업무 상세 설명
     */
    private String description;

    /**
     * 업무 내용 (리치 텍스트)
     */
    private String content;

    /**
     * 업무 상태 (NOT_STARTED, IN_PROGRESS, PENDING, COMPLETED, DELETED)
     * 기본값: NOT_STARTED
     */
    private String status;

    /**
     * 우선순위 (URGENT, HIGH, NORMAL, LOW)
     */
    private String priority;

    /**
     * 담당자 USERNAME
     */
    private String assigneeUsername;

    /**
     * 요청일
     */
    private LocalDate requestDate;

    /**
     * 마감일
     */
    private LocalDate dueDate;

    /**
     * 동적 속성값 (propertyId -> value)
     */
    private Map<Long, String> properties;
}
