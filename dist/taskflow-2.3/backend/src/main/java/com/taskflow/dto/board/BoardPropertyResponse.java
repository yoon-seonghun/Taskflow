package com.taskflow.dto.board;

import com.taskflow.domain.BoardProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 보드 속성 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardPropertyResponse {

    /**
     * 매핑 ID
     */
    private Long boardPropertyId;

    /**
     * 보드 ID
     */
    private Long boardId;

    /**
     * 속성 ID
     */
    private Long propertyId;

    /**
     * 속성명
     */
    private String propertyName;

    /**
     * 속성 타입
     */
    private String propertyType;

    /**
     * 속성 소유 유형 (GLOBAL/MANAGER/USER)
     */
    private String ownerType;

    /**
     * 필수 여부
     */
    private String requiredYn;

    /**
     * 표시 여부
     */
    private String visibleYn;

    /**
     * 정렬 순서
     */
    private Integer sortOrder;

    /**
     * 기본값
     */
    private String defaultValue;

    /**
     * 생성일시
     */
    private LocalDateTime createdAt;

    /**
     * 생성자
     */
    private String createdBy;

    /**
     * BoardProperty 엔티티를 DTO로 변환
     */
    public static BoardPropertyResponse from(BoardProperty entity) {
        if (entity == null) return null;

        return BoardPropertyResponse.builder()
                .boardPropertyId(entity.getBoardPropertyId())
                .boardId(entity.getBoardId())
                .propertyId(entity.getPropertyId())
                .propertyName(entity.getPropertyName())
                .propertyType(entity.getPropertyType())
                .ownerType(entity.getOwnerType())
                .requiredYn(entity.getRequiredYn())
                .visibleYn(entity.getVisibleYn())
                .sortOrder(entity.getSortOrder())
                .defaultValue(entity.getDefaultValue())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .build();
    }

    /**
     * BoardProperty 엔티티 리스트를 DTO 리스트로 변환
     */
    public static List<BoardPropertyResponse> fromList(List<BoardProperty> entities) {
        if (entities == null) return List.of();

        return entities.stream()
                .map(BoardPropertyResponse::from)
                .collect(Collectors.toList());
    }
}
