package com.taskflow.dto.template;

import com.taskflow.domain.TemplateProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 템플릿 속성 응답 DTO
 */
@Getter
@Builder
public class TemplatePropertyResponse {

    /**
     * 매핑 ID
     */
    private Long id;

    /**
     * 속성 ID
     */
    private Long propertyId;

    /**
     * 속성명
     */
    private String propertyName;

    /**
     * 속성 타입
     */
    private String propertyType;

    /**
     * 속성 소유자 타입 (GLOBAL, MANAGER, USER)
     */
    private String ownerType;

    /**
     * 정렬 순서
     */
    private Integer sortOrder;

    /**
     * 기본값
     */
    private String defaultValue;

    /**
     * 필수 여부 (Y/N)
     */
    private String requiredYn;

    /**
     * TemplateProperty 엔티티를 TemplatePropertyResponse로 변환
     */
    public static TemplatePropertyResponse from(TemplateProperty property) {
        return TemplatePropertyResponse.builder()
                .id(property.getId())
                .propertyId(property.getPropertyId())
                .propertyName(property.getPropertyName())
                .propertyType(property.getPropertyType())
                .ownerType(property.getOwnerType())
                .sortOrder(property.getSortOrder())
                .defaultValue(property.getDefaultValue())
                .requiredYn(property.getRequiredYn())
                .build();
    }

    /**
     * TemplateProperty 엔티티 리스트를 TemplatePropertyResponse 리스트로 변환
     */
    public static List<TemplatePropertyResponse> fromList(List<TemplateProperty> properties) {
        return properties.stream()
                .map(TemplatePropertyResponse::from)
                .collect(Collectors.toList());
    }
}
