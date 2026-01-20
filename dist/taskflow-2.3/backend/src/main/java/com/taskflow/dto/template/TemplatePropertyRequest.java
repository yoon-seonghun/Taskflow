package com.taskflow.dto.template;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 템플릿 속성 요청 DTO
 */
@Getter
@Setter
public class TemplatePropertyRequest {

    /**
     * 속성 ID
     */
    @NotNull(message = "속성 ID는 필수입니다")
    private Long propertyId;

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
}
