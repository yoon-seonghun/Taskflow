package com.taskflow.dto.category;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 카테고리 속성 매핑 요청 DTO
 *
 * v2.0 변경사항:
 * - requiredYn 필드 추가 (카테고리 레벨 필수 여부)
 */
@Getter
@Setter
public class CategoryPropertyRequest {

    /**
     * 속성 ID
     */
    @NotNull(message = "속성 ID는 필수입니다")
    private Long propertyId;

    /**
     * 정렬 순서
     */
    private Integer sortOrder;

    /**
     * 기본값
     */
    @Size(max = 500, message = "기본값은 500자 이하여야 합니다")
    private String defaultValue;

    /**
     * 필수 여부 (카테고리 레벨) - v2.0
     */
    @Pattern(regexp = "^[YN]$", message = "필수 여부는 Y 또는 N이어야 합니다")
    private String requiredYn = "N";

    /**
     * 일괄 속성 추가 요청 DTO
     */
    @Getter
    @Setter
    public static class BulkAddRequest {
        /**
         * 추가할 속성 ID 목록
         */
        private List<Long> propertyIds;
    }

    /**
     * 속성 순서 변경 요청 DTO
     */
    @Getter
    @Setter
    public static class OrderUpdateRequest {
        /**
         * 속성 ID와 순서 목록
         */
        private List<PropertyOrder> orders;

        @Getter
        @Setter
        public static class PropertyOrder {
            private Long propertyId;
            private Integer sortOrder;
        }
    }

    /**
     * 기본값 수정 요청 DTO
     */
    @Getter
    @Setter
    public static class DefaultValueUpdateRequest {
        /**
         * 기본값
         */
        @Size(max = 500, message = "기본값은 500자 이하여야 합니다")
        private String defaultValue;
    }

    /**
     * 필수 여부 수정 요청 DTO (v2.0)
     */
    @Getter
    @Setter
    public static class RequiredYnUpdateRequest {
        /**
         * 필수 여부
         */
        @Pattern(regexp = "^[YN]$", message = "필수 여부는 Y 또는 N이어야 합니다")
        private String requiredYn;
    }
}
