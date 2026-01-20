package com.taskflow.dto.board;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 보드 순서 변경 요청 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardOrderRequest {

    /**
     * 변경할 정렬 순서
     */
    @NotNull(message = "정렬 순서는 필수입니다.")
    private Integer sortOrder;
}
