package com.taskflow.dto.item;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 아이템 속성 순서 변경 요청 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemPropertySortRequest {

    /**
     * 속성 순서 목록
     */
    private List<PropertySortOrder> orders;

    /**
     * 속성 순서 정보
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PropertySortOrder {
        /**
         * 아이템 속성값 ID
         */
        private Long itemPropertyId;

        /**
         * 정렬 순서 (1부터 시작)
         */
        private Integer sortOrder;
    }
}
