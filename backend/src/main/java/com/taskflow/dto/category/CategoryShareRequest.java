package com.taskflow.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 카테고리 공유 요청 DTO
 */
@Getter
@Setter
public class CategoryShareRequest {

    /**
     * 공유 대상 유형 (USER, DEPARTMENT)
     */
    @NotBlank(message = "공유 대상 유형은 필수입니다")
    @Pattern(regexp = "^(USER|DEPARTMENT)$", message = "공유 대상 유형은 USER 또는 DEPARTMENT만 가능합니다")
    private String shareType;

    /**
     * 공유 대상 (USERNAME 또는 DEPT_CODE)
     */
    @NotBlank(message = "공유 대상은 필수입니다")
    private String shareTarget;

    /**
     * 권한 (READ, EDIT)
     */
    @NotBlank(message = "권한은 필수입니다")
    @Pattern(regexp = "^(READ|EDIT)$", message = "권한은 READ 또는 EDIT만 가능합니다")
    private String permission;

    /**
     * 일괄 공유 요청 DTO
     */
    @Getter
    @Setter
    public static class BulkShareRequest {
        /**
         * 공유 대상 목록
         */
        private List<CategoryShareRequest> shares;
    }
}
