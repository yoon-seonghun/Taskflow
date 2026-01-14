package com.taskflow.dto.content;

import com.taskflow.domain.ItemContent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 업무 내용 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemContentResponse {

    private Long contentId;
    private Long itemId;
    private String contentType;
    private String content;
    private String plainText;
    private Integer version;
    private LocalDateTime createdAt;
    private String createdBy;
    private LocalDateTime updatedAt;
    private String updatedBy;

    /**
     * Domain → DTO 변환
     */
    public static ItemContentResponse from(ItemContent entity) {
        if (entity == null) {
            return null;
        }
        return ItemContentResponse.builder()
                .contentId(entity.getContentId())
                .itemId(entity.getItemId())
                .contentType(entity.getContentType())
                .content(entity.getContent())
                .plainText(entity.getPlainText())
                .version(entity.getVersion())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .updatedAt(entity.getUpdatedAt())
                .updatedBy(entity.getUpdatedBy())
                .build();
    }
}
