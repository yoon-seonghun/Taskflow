package com.taskflow.dto.position;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 직급 수정 요청 DTO
 */
@Getter
@Setter
public class PositionUpdateRequest {

    /**
     * 직급명
     */
    @NotBlank(message = "직급명은 필수입니다")
    @Size(max = 50, message = "직급명은 50자를 초과할 수 없습니다")
    private String positionName;

    /**
     * 정렬 순서 (낮을수록 높은 직급)
     */
    private Integer sortOrder;

    /**
     * 사용 여부
     */
    private String useYn;
}
