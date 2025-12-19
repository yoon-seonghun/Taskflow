package com.taskflow.dto.group;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 그룹 순서 변경 요청 DTO
 */
@Getter
@Setter
public class GroupOrderRequest {

    /**
     * 정렬 순서
     */
    @NotNull(message = "정렬 순서는 필수입니다")
    @Min(value = 0, message = "정렬 순서는 0 이상이어야 합니다")
    private Integer sortOrder;
}
