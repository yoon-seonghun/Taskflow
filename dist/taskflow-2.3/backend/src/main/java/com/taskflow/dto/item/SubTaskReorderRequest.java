package com.taskflow.dto.item;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 하위 업무 순서 변경 요청 DTO (v2.2)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubTaskReorderRequest {

    /**
     * 순서 변경 목록
     */
    @NotNull(message = "순서 정보는 필수입니다.")
    private List<OrderItem> orders;

    /**
     * 순서 항목
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItem {
        /**
         * 업무 ID
         */
        @NotNull(message = "업무 ID는 필수입니다.")
        private Long itemId;

        /**
         * 정렬 순서
         */
        @NotNull(message = "정렬 순서는 필수입니다.")
        private Integer sortOrder;
    }
}
