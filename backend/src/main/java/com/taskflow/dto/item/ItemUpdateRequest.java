package com.taskflow.dto.item;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 아이템 수정 요청 DTO
 */
@Getter
@Setter
public class ItemUpdateRequest {

    /**
     * 아이템 제목/내용
     */
    @Size(max = 500, message = "제목은 500자 이내여야 합니다")
    private String title;

    /**
     * 제목 (content 동일, 호환성용)
     */
    @Size(max = 500, message = "내용은 500자 이내여야 합니다")
    private String content;

    /**
     * 상세 내용 (마크다운)
     */
    private String description;

    /**
     * 그룹 ID
     */
    private Long groupId;

    /**
     * 카테고리 ID
     */
    private Long categoryId;

    /**
     * 상태 (NOT_STARTED, IN_PROGRESS, COMPLETED, DELETED)
     */
    @Pattern(regexp = "^(NOT_STARTED|IN_PROGRESS|COMPLETED|DELETED)$",
            message = "유효하지 않은 상태입니다")
    private String status;

    /**
     * 우선순위 (URGENT, HIGH, NORMAL, LOW)
     */
    @Pattern(regexp = "^(URGENT|HIGH|NORMAL|LOW)$",
            message = "유효하지 않은 우선순위입니다")
    private String priority;

    /**
     * 담당자 ID
     */
    private Long assigneeId;

    /**
     * 시작 시간
     */
    private LocalDateTime startTime;

    /**
     * 완료 시간
     */
    private LocalDateTime endTime;

    /**
     * 마감일
     */
    private LocalDateTime dueDate;

    /**
     * 정렬 순서
     */
    private Integer sortOrder;

    /**
     * 동적 속성값 (propertyId -> value)
     */
    private Map<Long, Object> properties;
}
