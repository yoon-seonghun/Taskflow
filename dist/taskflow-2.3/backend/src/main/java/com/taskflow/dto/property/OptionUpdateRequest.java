package com.taskflow.dto.property;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 옵션 수정 요청 DTO
 */
@Getter
@Setter
public class OptionUpdateRequest {

    /**
     * 옵션명 (표시값)
     */
    @NotBlank(message = "옵션명은 필수입니다")
    @Size(max = 100, message = "옵션명은 100자 이내여야 합니다")
    private String optionName;

    /**
     * 표시 색상 (#RRGGBB)
     */
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "색상 형식이 올바르지 않습니다 (예: #FF0000)")
    private String color;

    /**
     * 표시 순서
     */
    private Integer sortOrder;

    /**
     * 사용 여부 (Y/N)
     */
    @Pattern(regexp = "^[YN]$", message = "사용 여부는 Y 또는 N이어야 합니다")
    private String useYn;
}
