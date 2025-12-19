package com.taskflow.dto.property;

import com.taskflow.domain.PropertyDef;
import com.taskflow.domain.PropertyOption;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 속성 정의 응답 DTO
 */
@Getter
@Builder
public class PropertyResponse {

    /**
     * 속성 정의 ID
     */
    private Long propertyId;

    /**
     * 보드 ID
     */
    private Long boardId;

    /**
     * 속성명
     */
    private String propertyName;

    /**
     * 속성 타입
     */
    private String propertyType;

    /**
     * 필수 여부
     */
    private String requiredYn;

    /**
     * 표시 순서
     */
    private Integer sortOrder;

    /**
     * 표시 여부
     */
    private String visibleYn;

    /**
     * 삭제 여부 (SSE 이벤트용)
     */
    private boolean deleted;

    /**
     * 옵션 목록 (SELECT, MULTI_SELECT 타입용)
     */
    private List<OptionResponse> options;

    /**
     * 생성일시
     */
    private LocalDateTime createdAt;

    /**
     * 수정일시
     */
    private LocalDateTime updatedAt;

    /**
     * 도메인 객체를 응답 DTO로 변환
     */
    public static PropertyResponse from(PropertyDef propertyDef) {
        if (propertyDef == null) {
            return null;
        }

        List<OptionResponse> optionResponses = null;
        if (propertyDef.getOptions() != null && !propertyDef.getOptions().isEmpty()) {
            optionResponses = propertyDef.getOptions().stream()
                    .map(OptionResponse::from)
                    .collect(Collectors.toList());
        }

        return PropertyResponse.builder()
                .propertyId(propertyDef.getPropertyId())
                .boardId(propertyDef.getBoardId())
                .propertyName(propertyDef.getPropertyName())
                .propertyType(propertyDef.getPropertyType())
                .requiredYn(propertyDef.getRequiredYn())
                .sortOrder(propertyDef.getSortOrder())
                .visibleYn(propertyDef.getVisibleYn())
                .options(optionResponses)
                .createdAt(propertyDef.getCreatedAt())
                .updatedAt(propertyDef.getUpdatedAt())
                .build();
    }

    /**
     * 도메인 객체 리스트를 응답 DTO 리스트로 변환
     */
    public static List<PropertyResponse> fromList(List<PropertyDef> properties) {
        if (properties == null) {
            return List.of();
        }

        return properties.stream()
                .map(PropertyResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 옵션 응답 DTO (내부 클래스)
     */
    @Getter
    @Builder
    public static class OptionResponse {

        private Long optionId;
        private String optionName;
        private String color;
        private Integer sortOrder;
        private String useYn;

        public static OptionResponse from(PropertyOption option) {
            if (option == null) {
                return null;
            }

            return OptionResponse.builder()
                    .optionId(option.getOptionId())
                    .optionName(option.getOptionName())
                    .color(option.getColor())
                    .sortOrder(option.getSortOrder())
                    .useYn(option.getUseYn())
                    .build();
        }
    }
}
