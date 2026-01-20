package com.taskflow.dto.template;

import com.taskflow.domain.TaskTemplate;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 작업 템플릿 검색(자동완성) 응답 DTO
 */
@Getter
@Builder
public class TaskTemplateSearchResponse {

    /**
     * 템플릿 ID
     */
    private Long templateId;

    /**
     * 작업 내용
     */
    private String content;

    /**
     * 사용 횟수
     */
    private Integer useCount;

    /**
     * TaskTemplate 엔티티를 TaskTemplateSearchResponse로 변환
     */
    public static TaskTemplateSearchResponse from(TaskTemplate template) {
        return TaskTemplateSearchResponse.builder()
                .templateId(template.getTemplateId())
                .content(template.getContent())
                .useCount(template.getUseCount())
                .build();
    }

    /**
     * TaskTemplate 엔티티 리스트를 TaskTemplateSearchResponse 리스트로 변환
     */
    public static List<TaskTemplateSearchResponse> fromList(List<TaskTemplate> templates) {
        return templates.stream()
                .map(TaskTemplateSearchResponse::from)
                .collect(Collectors.toList());
    }
}
