package com.taskflow.dto.group;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 그룹 수정 요청 DTO
 */
@Getter
@Setter
public class GroupUpdateRequest {

    /**
     * 그룹명
     */
    @NotBlank(message = "그룹명은 필수입니다")
    @Size(max = 50, message = "그룹명은 50자 이내여야 합니다")
    private String groupName;

    /**
     * 그룹 설명
     */
    @Size(max = 200, message = "그룹 설명은 200자 이내여야 합니다")
    private String description;

    /**
     * 표시 색상 (#RRGGBB)
     */
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "색상 형식이 올바르지 않습니다 (예: #FF0000)")
    private String color;

    /**
     * 정렬 순서
     */
    private Integer sortOrder;

    /**
     * 사용 여부 (Y/N)
     */
    @Pattern(regexp = "^[YN]$", message = "사용 여부는 Y 또는 N이어야 합니다")
    private String useYn;
}
