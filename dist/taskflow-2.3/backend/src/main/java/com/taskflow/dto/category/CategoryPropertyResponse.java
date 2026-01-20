package com.taskflow.dto.category;

import com.taskflow.domain.CategoryProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 카테고리 속성 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryPropertyResponse {

    /**
     * 매핑 ID
     */
    private Long categoryPropertyId;

    /**
     * 카테고리 ID
     */
    private Long categoryId;

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
     * 속성 소유 유형 (GLOBAL/MANAGER/USER)
     */
    private String ownerType;

    /**
     * 필수 여부
     */
    private String requiredYn;

    /**
     * 정렬 순서
     */
    private Integer sortOrder;

    /**
     * 기본값
     */
    private String defaultValue;

    /**
     * 생성일시
     */
    private LocalDateTime createdAt;

    /**
     * 생성자
     */
    private String createdBy;

    /**
     * CategoryProperty 엔티티를 DTO로 변환
     */
    public static CategoryPropertyResponse from(CategoryProperty entity) {
        if (entity == null) return null;

        return CategoryPropertyResponse.builder()
                .categoryPropertyId(entity.getCategoryPropertyId())
                .categoryId(entity.getCategoryId())
                .propertyId(entity.getPropertyId())
                .propertyName(entity.getPropertyName())
                .propertyType(entity.getPropertyType())
                .ownerType(entity.getOwnerType())
                .requiredYn(entity.getRequiredYn())
                .sortOrder(entity.getSortOrder())
                .defaultValue(entity.getDefaultValue())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .build();
    }

    /**
     * CategoryProperty 엔티티 리스트를 DTO 리스트로 변환
     */
    public static List<CategoryPropertyResponse> fromList(List<CategoryProperty> entities) {
        if (entities == null) return List.of();

        return entities.stream()
                .map(CategoryPropertyResponse::from)
                .collect(Collectors.toList());
    }
}
