package com.taskflow.dto.category;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 보드 속성 매핑 요청 DTO
 */
@Getter
@Setter
public class BoardPropertyRequest {

    /**
     * 속성 ID
     */
    @NotNull(message = "속성 ID는 필수입니다")
    private Long propertyId;

    /**
     * 필수 여부 (Y/N)
     */
    private String requiredYn;

    /**
     * 표시 여부 (Y/N)
     */
    private String visibleYn;

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
     * 일괄 속성 등록 요청 DTO
     */
    @Getter
    @Setter
    public static class BulkRegisterRequest {
        /**
         * 등록할 속성 목록
         */
        private List<BoardPropertyRequest> properties;
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
}
