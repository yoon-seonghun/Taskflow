package com.taskflow.dto.template;

import com.taskflow.domain.TaskTemplate;
import com.taskflow.domain.TemplateProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Collections;
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
     * 카테고리 ID
     */
    private Long categoryId;

    /**
     * 카테고리명
     */
    private String categoryName;

    /**
     * 카테고리 색상
     */
    private String categoryColor;

    /**
     * 소유 유형 (GLOBAL, MANAGER, USER)
     */
    private String ownerType;

    /**
     * 소유자 USERNAME
     */
    private String ownerUsername;

    /**
     * 소유자 부서코드
     */
    private String ownerDeptCode;

    /**
     * 기본 담당자 USERNAME
     */
    private String defaultAssigneeUsername;

    /**
     * 기본 담당자 이름
     */
    private String defaultAssigneeName;

    /**
     * 기본 업무 상태
     */
    private String defaultItemStatus;

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
     * 생성자 USERNAME
     */
    private String createdBy;

    /**
     * 생성자 이름
     */
    private String createdByName;

    /**
     * 수정일시
     */
    private LocalDateTime updatedAt;

    /**
     * 수정자 USERNAME
     */
    private String updatedBy;

    /**
     * 수정자 이름
     */
    private String updatedByName;

    /**
     * 템플릿 속성 목록
     */
    private List<TemplatePropertyResponse> properties;

    /**
     * TaskTemplate 엔티티를 TaskTemplateResponse로 변환
     */
    public static TaskTemplateResponse from(TaskTemplate template) {
        return TaskTemplateResponse.builder()
                .templateId(template.getTemplateId())
                .content(template.getContent())
                .status(template.getStatus())
                .categoryId(template.getCategoryId())
                .categoryName(template.getCategoryName())
                .categoryColor(template.getCategoryColor())
                .ownerType(template.getOwnerType())
                .ownerUsername(template.getOwnerUsername())
                .ownerDeptCode(template.getOwnerDeptCode())
                .defaultAssigneeUsername(template.getDefaultAssigneeUsername())
                .defaultAssigneeName(template.getDefaultAssigneeName())
                .defaultItemStatus(template.getDefaultItemStatus())
                .sortOrder(template.getSortOrder())
                .useCount(template.getUseCount())
                .createdAt(template.getCreatedAt())
                .createdBy(template.getCreatedBy())
                .createdByName(template.getCreatedByName())
                .updatedAt(template.getUpdatedAt())
                .updatedBy(template.getUpdatedBy())
                .updatedByName(template.getUpdatedByName())
                .properties(Collections.emptyList())
                .build();
    }

    /**
     * TaskTemplate 엔티티와 속성 목록을 TaskTemplateResponse로 변환
     */
    public static TaskTemplateResponse from(TaskTemplate template, List<TemplateProperty> templateProperties) {
        return TaskTemplateResponse.builder()
                .templateId(template.getTemplateId())
                .content(template.getContent())
                .status(template.getStatus())
                .categoryId(template.getCategoryId())
                .categoryName(template.getCategoryName())
                .categoryColor(template.getCategoryColor())
                .ownerType(template.getOwnerType())
                .ownerUsername(template.getOwnerUsername())
                .ownerDeptCode(template.getOwnerDeptCode())
                .defaultAssigneeUsername(template.getDefaultAssigneeUsername())
                .defaultAssigneeName(template.getDefaultAssigneeName())
                .defaultItemStatus(template.getDefaultItemStatus())
                .sortOrder(template.getSortOrder())
                .useCount(template.getUseCount())
                .createdAt(template.getCreatedAt())
                .createdBy(template.getCreatedBy())
                .createdByName(template.getCreatedByName())
                .updatedAt(template.getUpdatedAt())
                .updatedBy(template.getUpdatedBy())
                .updatedByName(template.getUpdatedByName())
                .properties(templateProperties != null ?
                        TemplatePropertyResponse.fromList(templateProperties) : Collections.emptyList())
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
