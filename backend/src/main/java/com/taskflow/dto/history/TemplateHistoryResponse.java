package com.taskflow.dto.history;

import com.taskflow.domain.TaskTemplate;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 작업 등록 이력 응답 DTO
 */
@Getter
@Builder
public class TemplateHistoryResponse {

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
     * 등록자 ID
     */
    private Long createdBy;

    /**
     * 등록자 이름
     */
    private String createdByName;

    /**
     * 등록시간
     */
    private LocalDateTime createdAt;

    /**
     * 수정시간
     */
    private LocalDateTime updatedAt;

    /**
     * 수정자 이름
     */
    private String updatedByName;

    /**
     * TaskTemplate을 TemplateHistoryResponse로 변환
     */
    public static TemplateHistoryResponse from(TaskTemplate template) {
        return TemplateHistoryResponse.builder()
                .templateId(template.getTemplateId())
                .content(template.getContent())
                .status(template.getStatus())
                .createdBy(template.getCreatedBy())
                .createdByName(template.getCreatedByName())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .updatedByName(template.getUpdatedByName())
                .build();
    }

    /**
     * TaskTemplate 리스트를 TemplateHistoryResponse 리스트로 변환
     */
    public static List<TemplateHistoryResponse> fromList(List<TaskTemplate> templates) {
        return templates.stream()
                .map(TemplateHistoryResponse::from)
                .collect(Collectors.toList());
    }
}
