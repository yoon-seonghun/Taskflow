package com.taskflow.dto.item;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 아이템 순서 변경 요청 DTO
 */
@Getter
@Setter
public class ItemReorderRequest {

    /**
     * 이동할 아이템 ID
     */
    @NotNull(message = "아이템 ID는 필수입니다")
    private Long itemId;

    /**
     * 새 순서 (0부터 시작)
     */
    @NotNull(message = "새 순서는 필수입니다")
    @Min(value = 0, message = "순서는 0 이상이어야 합니다")
    private Integer newOrder;
}
