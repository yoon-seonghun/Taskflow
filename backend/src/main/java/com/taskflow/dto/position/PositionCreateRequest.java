package com.taskflow.dto.position;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 직급 등록 요청 DTO
 */
@Getter
@Setter
public class PositionCreateRequest {

    /**
     * 직급 코드 (영문+숫자, 2~20자)
     */
    @NotBlank(message = "직급 코드는 필수입니다")
    @Size(min = 2, max = 20, message = "직급 코드는 2~20자 사이여야 합니다")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "직급 코드는 영문 대문자, 숫자, 언더스코어만 사용 가능합니다")
    private String positionCode;

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
}
