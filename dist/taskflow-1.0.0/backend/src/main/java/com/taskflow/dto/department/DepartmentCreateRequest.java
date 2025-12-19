package com.taskflow.dto.department;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 부서 등록 요청 DTO
 */
@Getter
@Setter
public class DepartmentCreateRequest {

    /**
     * 부서 코드 (영문+숫자, 2~20자)
     */
    @NotBlank(message = "부서 코드는 필수입니다")
    @Size(min = 2, max = 20, message = "부서 코드는 2~20자 사이여야 합니다")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "부서 코드는 영문 대문자, 숫자, 언더스코어만 사용 가능합니다")
    private String departmentCode;

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
}
