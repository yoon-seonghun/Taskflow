package com.taskflow.dto.category;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 카테고리 수정 요청 DTO
 *
 * v2.0 변경사항:
 * - categoryCode, description 필드 추가
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryUpdateRequest {

    /**
     * 카테고리 코드 (고유 식별자)
     */
    @Size(max = 50, message = "카테고리 코드는 50자 이하여야 합니다")
    @Pattern(regexp = "^[A-Z0-9_]*$", message = "카테고리 코드는 영문 대문자, 숫자, 밑줄만 사용 가능합니다")
    private String categoryCode;

    /**
     * 카테고리명
     */
    @Size(max = 100, message = "카테고리명은 100자 이하여야 합니다")
    private String categoryName;

    /**
     * 설명
     */
    @Size(max = 500, message = "설명은 500자 이하여야 합니다")
    private String description;

    /**
     * 표시 색상 (#RRGGBB)
     */
    @Size(max = 20, message = "색상은 20자 이하여야 합니다")
    private String categoryColor;

    /**
     * 정렬 순서
     */
    private Integer sortOrder;

    /**
     * 사용 여부 (Y/N)
     */
    private String useYn;
}
