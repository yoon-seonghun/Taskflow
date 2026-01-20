package com.taskflow.dto.board;

import com.taskflow.domain.BoardCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 보드 카테고리 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardCategoryResponse {

    private Long boardCategoryId;
    private Long boardId;
    private Long categoryId;
    private String categoryName;
    private String categoryColor;
    private Integer sortOrder;
    private Boolean isDefault;
    private LocalDateTime createdAt;

    public static BoardCategoryResponse from(BoardCategory entity) {
        if (entity == null) return null;

        return BoardCategoryResponse.builder()
                .boardCategoryId(entity.getBoardCategoryId())
                .boardId(entity.getBoardId())
                .categoryId(entity.getCategoryId())
                .categoryName(entity.getCategoryName())
                .categoryColor(entity.getCategoryColor())
                .sortOrder(entity.getSortOrder())
                .isDefault("Y".equals(entity.getDefaultYn()))
                .createdAt(entity.getCreatedAt())
                .build();
    }

    public static List<BoardCategoryResponse> fromList(List<BoardCategory> entities) {
        if (entities == null) return List.of();
        return entities.stream()
                .map(BoardCategoryResponse::from)
                .collect(Collectors.toList());
    }
}
