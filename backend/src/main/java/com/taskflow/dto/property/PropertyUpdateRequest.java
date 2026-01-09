package com.taskflow.dto.property;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 속성 정의 수정 요청 DTO
 *
 * v2.0 변경사항:
 * - requiredYn 필드 제거 (CategoryProperty/BoardProperty로 이동)
 * - ownerType 등은 생성 후 변경 불가 (별도 API 필요시 추가)
 */
@Getter
@Setter
public class PropertyUpdateRequest {

    /**
     * 속성명
     */
    @Size(max = 100, message = "속성명은 100자 이내여야 합니다")
    private String propertyName;

    /**
     * 속성 타입 (TEXT, NUMBER, DATE, SELECT, MULTI_SELECT, CHECKBOX, USER)
     */
    @Pattern(regexp = "^(TEXT|NUMBER|DATE|SELECT|MULTI_SELECT|CHECKBOX|USER)$",
            message = "유효하지 않은 속성 타입입니다")
    private String propertyType;

    /**
     * 표시 순서
     */
    private Integer sortOrder;

    /**
     * 표시 여부 (Y/N)
     */
    @Pattern(regexp = "^[YN]$", message = "표시 여부는 Y 또는 N이어야 합니다")
    private String visibleYn;
}
