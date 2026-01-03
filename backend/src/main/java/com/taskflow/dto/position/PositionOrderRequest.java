package com.taskflow.dto.position;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 직급 순서 변경 요청 DTO
 */
@Getter
@Setter
public class PositionOrderRequest {

    /**
     * 직급 코드 목록 (순서대로)
     */
    @NotEmpty(message = "직급 코드 목록은 필수입니다")
    private List<String> positionCodes;
}
