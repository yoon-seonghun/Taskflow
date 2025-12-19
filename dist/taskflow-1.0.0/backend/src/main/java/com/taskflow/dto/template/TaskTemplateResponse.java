package com.taskflow.dto.template;

import com.taskflow.domain.TaskTemplate;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 작업 템플릿 응답 DTO
 */
@Getter
@Builder
public class TaskTemplateResponse {

    /**
     * 템플릿 ID
     */
    private Long templateId;

    /**
     * 작업 내용
     */
    private String content;

    /**
     * 상태
     */
    private String status;

    /**
     * 정렬 순서
     */
    private Integer sortOrder;

    /**
     * 사용 횟수
     */
    private Integer useCount;

    /**
     * 생성일시
     */
    private LocalDateTime createdAt;

    /**
     * 생성자 ID
     */
    private Long createdBy;

    /**
     * 생성자 이름
     */
    private String createdByName;

    /**
     * 수정일시
     */
    private LocalDateTime updatedAt;

    /**
     * 수정자 ID
     */
    private Long updatedBy;

    /**
     * 수정자 이름
     */
    private String updatedByName;

    /**
     * TaskTemplate 엔티티를 TaskTemplateResponse로 변환
     */
    public static TaskTemplateResponse from(TaskTemplate template) {
        return TaskTemplateResponse.builder()
                .templateId(template.getTemplateId())
                .content(template.getContent())
                .status(template.getStatus())
                .sortOrder(template.getSortOrder())
                .useCount(template.getUseCount())
                .createdAt(template.getCreatedAt())
                .createdBy(template.getCreatedBy())
                .createdByName(template.getCreatedByName())
                .updatedAt(template.getUpdatedAt())
                .updatedBy(template.getUpdatedBy())
                .updatedByName(template.getUpdatedByName())
                .build();
    }

    /**
     * TaskTemplate 엔티티 리스트를 TaskTemplateResponse 리스트로 변환
     */
    public static List<TaskTemplateResponse> fromList(List<TaskTemplate> templates) {
        return templates.stream()
                .map(TaskTemplateResponse::from)
                .collect(Collectors.toList());
    }
}
