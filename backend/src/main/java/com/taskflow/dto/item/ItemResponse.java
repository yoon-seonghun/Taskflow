package com.taskflow.dto.item;

import com.taskflow.domain.Item;
import com.taskflow.domain.ItemProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 아이템 응답 DTO
 */
@Getter
@Builder
public class ItemResponse {

    /**
     * 아이템 ID
     */
    private Long itemId;

    /**
     * 보드 ID
     */
    private Long boardId;

    /**
     * 보드명
     */
    private String boardName;

    /**
     * 그룹 ID
     */
    private Long groupId;

    /**
     * 그룹명
     */
    private String groupName;

    /**
     * 그룹 색상
     */
    private String groupColor;

    /**
     * 카테고리 ID
     */
    private Long categoryId;

    /**
     * 제목
     */
    private String title;

    /**
     * 내용 (제목과 동일, 호환성)
     */
    private String content;

    /**
     * 상세 내용 (마크다운)
     */
    private String description;

    /**
     * 상태
     */
    private String status;

    /**
     * 우선순위
     */
    private String priority;

    /**
     * 담당자 ID
     */
    private Long assigneeId;

    /**
     * 담당자명
     */
    private String assigneeName;

    /**
     * 시작 시간
     */
    private LocalDateTime startTime;

    /**
     * 완료 시간
     */
    private LocalDateTime endTime;

    /**
     * 마감일
     */
    private LocalDateTime dueDate;

    /**
     * 정렬 순서
     */
    private Integer sortOrder;

    /**
     * 완료일시
     */
    private LocalDateTime completedAt;

    /**
     * 삭제일시
     */
    private LocalDateTime deletedAt;

    /**
     * 댓글 수
     */
    private Integer commentCount;

    /**
     * 생성자명
     */
    private String createdByName;

    /**
     * 수정자명
     */
    private String updatedByName;

    /**
     * 생성일시
     */
    private LocalDateTime createdAt;

    /**
     * 수정일시
     */
    private LocalDateTime updatedAt;

    /**
     * 동적 속성값 목록
     */
    private List<PropertyValueResponse> properties;

    /**
     * 동적 속성값 맵 (propertyId -> value)
     */
    private Map<Long, Object> propertyValues;

    /**
     * 도메인 객체를 응답 DTO로 변환
     */
    public static ItemResponse from(Item item) {
        if (item == null) {
            return null;
        }

        List<PropertyValueResponse> propertyResponses = null;
        Map<Long, Object> propertyValueMap = new HashMap<>();

        if (item.getProperties() != null && !item.getProperties().isEmpty()) {
            propertyResponses = item.getProperties().stream()
                    .map(PropertyValueResponse::from)
                    .collect(Collectors.toList());

            for (ItemProperty prop : item.getProperties()) {
                propertyValueMap.put(prop.getPropertyId(), prop.getValue());
            }
        }

        if (item.getPropertyValues() != null) {
            propertyValueMap.putAll(item.getPropertyValues());
        }

        return ItemResponse.builder()
                .itemId(item.getItemId())
                .boardId(item.getBoardId())
                .boardName(item.getBoardName())
                .groupId(item.getGroupId())
                .groupName(item.getGroupName())
                .groupColor(item.getGroupColor())
                .categoryId(item.getCategoryId())
                .title(item.getTitle())
                .content(item.getContent())
                .description(item.getDescription())
                .status(item.getStatus())
                .priority(item.getPriority())
                .assigneeId(item.getAssigneeId())
                .assigneeName(item.getAssigneeName())
                .startTime(item.getStartTime())
                .endTime(item.getEndTime())
                .deletedAt(item.getDeletedAt())
                .commentCount(item.getCommentCount())
                .createdByName(item.getCreatedByName())
                .updatedByName(item.getUpdatedByName())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .properties(propertyResponses)
                .propertyValues(propertyValueMap.isEmpty() ? null : propertyValueMap)
                .build();
    }

    /**
     * 도메인 객체 리스트를 응답 DTO 리스트로 변환
     */
    public static List<ItemResponse> fromList(List<Item> items) {
        if (items == null) {
            return List.of();
        }

        return items.stream()
                .map(ItemResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 속성값 응답 DTO (내부 클래스)
     */
    @Getter
    @Builder
    public static class PropertyValueResponse {

        private Long propertyId;
        private String propertyName;
        private String propertyType;
        private Object value;
        private String displayValue;  // 표시용 값 (옵션명, 사용자명 등)
        private String color;         // 옵션 색상

        public static PropertyValueResponse from(ItemProperty prop) {
            if (prop == null) {
                return null;
            }

            String displayValue = null;
            if (prop.getOptionName() != null) {
                displayValue = prop.getOptionName();
            } else if (prop.getValueUserName() != null) {
                displayValue = prop.getValueUserName();
            }

            return PropertyValueResponse.builder()
                    .propertyId(prop.getPropertyId())
                    .propertyName(prop.getPropertyName())
                    .propertyType(prop.getPropertyType())
                    .value(prop.getValue())
                    .displayValue(displayValue)
                    .color(prop.getOptionColor())
                    .build();
        }
    }
}
