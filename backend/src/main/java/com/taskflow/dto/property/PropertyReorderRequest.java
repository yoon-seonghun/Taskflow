package com.taskflow.dto.property;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 속성 순서 변경 요청 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyReorderRequest {

    /**
     * 소유 유형 (GLOBAL, MANAGER, USER)
     * - 같은 유형 내에서만 순서 변경 가능
     */
    @NotNull(message = "소유 유형은 필수입니다")
    private String ownerType;

    /**
     * 순서 변경 목록
     */
    @NotEmpty(message = "순서 목록은 필수입니다")
    @Valid
    private List<PropertyOrder> orders;

    /**
     * 개별 속성 순서 정보
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PropertyOrder {

        @NotNull(message = "속성 ID는 필수입니다")
        private Long propertyId;

        @NotNull(message = "정렬 순서는 필수입니다")
        private Integer sortOrder;
    }
}
