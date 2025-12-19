package com.taskflow.dto.template;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 작업 템플릿 생성 요청 DTO
 */
@Getter
@Setter
public class TaskTemplateCreateRequest {

    /**
     * 작업 내용
     */
    @NotBlank(message = "작업 내용은 필수입니다")
    @Size(max = 500, message = "작업 내용은 500자를 초과할 수 없습니다")
    private String content;

    /**
     * 기본 담당자 ID
     */
    private Long defaultAssigneeId;

    /**
     * 기본 업무 상태 (NOT_STARTED, IN_PROGRESS, PENDING)
     */
    private String defaultItemStatus;

    /**
     * 정렬 순서
     */
    private Integer sortOrder;
}
