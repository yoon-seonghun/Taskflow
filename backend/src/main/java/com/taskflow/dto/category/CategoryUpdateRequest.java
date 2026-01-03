package com.taskflow.dto.category;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 카테고리 수정 요청 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryUpdateRequest {

    @Size(max = 100, message = "카테고리명은 100자 이하여야 합니다")
    private String categoryName;

    @Size(max = 20, message = "색상은 20자 이하여야 합니다")
    private String color;

    private Integer sortOrder;

    private String useYn;
}
