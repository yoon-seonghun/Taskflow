package com.taskflow.dto.department;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 부서 수정 요청 DTO
 */
@Getter
@Setter
public class DepartmentUpdateRequest {

    /**
     * 부서명
     */
    @NotBlank(message = "부서명은 필수입니다")
    @Size(max = 100, message = "부서명은 100자를 초과할 수 없습니다")
    private String departmentName;

    /**
     * 상위 부서 ID (NULL = 최상위)
     */
    private Long parentId;

    /**
     * 정렬 순서
     */
    private Integer sortOrder;

    /**
     * 사용 여부 (Y/N)
     */
    private String useYn;
}
