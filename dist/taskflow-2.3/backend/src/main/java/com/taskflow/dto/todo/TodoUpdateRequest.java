package com.taskflow.dto.todo;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Todo 수정 요청 DTO
 */
@Getter
@Setter
public class TodoUpdateRequest {

    /**
     * Todo 제목
     */
    @Size(max = 500, message = "제목은 500자 이내여야 합니다")
    private String title;

    /**
     * 메모/상세 설명
     */
    private String description;

    /**
     * 우선순위 (URGENT, HIGH, NORMAL, LOW)
     */
    @Pattern(regexp = "^(URGENT|HIGH|NORMAL|LOW)$",
            message = "유효하지 않은 우선순위입니다")
    private String priority;

    /**
     * 마감일
     */
    private LocalDate dueDate;

    /**
     * 마감 시간
     */
    private LocalTime dueTime;

    /**
     * 완료 여부
     */
    private Boolean isCompleted;

    /**
     * 정렬 순서
     */
    private Integer sortOrder;

    /**
     * 반복 유형 (NONE/DAILY/WEEKLY/MONTHLY/YEARLY)
     */
    @Pattern(regexp = "^(NONE|DAILY|WEEKLY|MONTHLY|YEARLY)$",
            message = "유효하지 않은 반복 유형입니다")
    private String repeatType;

    /**
     * 반복 간격
     */
    private Integer repeatInterval;

    /**
     * 반복 요일 (1,2,3,4,5)
     */
    private String repeatDays;

    /**
     * 반복 종료일
     */
    private LocalDate repeatEndDate;

    /**
     * 그룹 ID (FK → TB_GROUP)
     */
    private Long groupId;
}
