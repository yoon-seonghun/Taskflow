package com.taskflow.dto.item;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Map;

/**
 * 아이템 생성 요청 DTO
 */
@Getter
@Setter
public class ItemCreateRequest {

    /**
     * 아이템 제목/내용
     */
    @NotBlank(message = "제목은 필수입니다")
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
     * 상태 (NOT_STARTED, IN_PROGRESS, PENDING, COMPLETED, DELETED)
     */
    @Pattern(regexp = "^(NOT_STARTED|IN_PROGRESS|PENDING|COMPLETED|DELETED)$",
            message = "유효하지 않은 상태입니다")
    private String status;

    /**
     * 우선순위 (URGENT, HIGH, NORMAL, LOW)
     */
    @Pattern(regexp = "^(URGENT|HIGH|NORMAL|LOW)$",
            message = "유효하지 않은 우선순위입니다")
    private String priority;

    /**
     * 담당자 아이디
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
     * 정렬 순서
     */
    private Integer sortOrder;

    /**
     * 동적 속성값 (propertyId -> value)
     */
    private Map<Long, Object> properties;

    /**
     * 속성 정렬 순서 (propertyId -> sortOrder)
     */
    private Map<Long, Integer> propertySortOrders;
}
