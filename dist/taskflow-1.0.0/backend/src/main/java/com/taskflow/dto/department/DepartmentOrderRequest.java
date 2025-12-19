package com.taskflow.dto.department;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 부서 순서 변경 요청 DTO
 */
@Getter
@Setter
public class DepartmentOrderRequest {

    /**
     * 새로운 정렬 순서
     */
    @NotNull(message = "정렬 순서는 필수입니다")
    private Integer sortOrder;

    /**
     * 새로운 상위 부서 ID (NULL = 최상위로 이동)
     */
    private Long parentId;
}
