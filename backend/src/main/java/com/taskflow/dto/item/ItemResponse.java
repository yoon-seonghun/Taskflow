package com.taskflow.dto.item;

import com.taskflow.domain.Item;
import com.taskflow.domain.ItemProperty;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
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
     * 카테고리명
     */
    private String categoryName;

    /**
     * 카테고리 색상
     */
    private String categoryColor;

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
     * 담당자 USERNAME
     */
    private String assigneeUsername;

    /**
     * 담당자명
     */
    private String assigneeName;

    /**
     * 요청일
     */
    private LocalDate requestDate;

    /**
     * 마감일
     */
    private LocalDate dueDate;

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

    // =============================================
    // 공유/이관 정보
    // =============================================

    /**
     * 공유받은 업무 여부 (현재 사용자 기준)
     */
    private Boolean isSharedToMe;

    /**
     * 공유해준 사용자 USERNAME
     */
    private String sharedByUsername;

    /**
     * 공유해준 사용자 이름
     */
    private String sharedByUserName;

    /**
     * 이관 원본 보드 ID
     */
    private Long transferredFrom;

    /**
     * 이관해준 사용자 USERNAME
     */
    private String transferredByUsername;

    /**
     * 이관해준 사용자 이름
     */
    private String transferredByUserName;

    // =============================================
    // 배정 정보 (v2.1)
    // =============================================

    /**
     * 배정받은 업무 여부 (현재 사용자 기준)
     */
    private Boolean isAssignedToMe;

    /**
     * 배정해준 사용자 USERNAME
     */
    private String assignedByUsername;

    /**
     * 배정해준 사용자 이름
     */
    private String assignedByUserName;

    /**
     * 배정일시
     */
    private LocalDateTime assignedAt;

    /**
     * 배정 대상 USERNAME (소유자 화면용)
     */
    private String assignedToUsername;

    /**
     * 배정 대상 이름 (소유자 화면용)
     */
    private String assignedToUserName;

    /**
     * 생성자 USERNAME (소유자 확인용)
     */
    private String createdBy;

    // =============================================
    // 접근 권한 정보 (v2.1 배정 기능)
    // =============================================

    /**
     * 현재 사용자의 접근 권한 정보
     */
    private ItemAccessInfo accessInfo;

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
                .categoryName(item.getCategoryName())
                .categoryColor(item.getCategoryColor())
                .title(item.getTitle())
                .content(item.getContent())
                .description(item.getDescription())
                .status(item.getStatus())
                .priority(item.getPriority())
                .assigneeUsername(item.getAssigneeUsername())
                .assigneeName(item.getAssigneeName())
                .requestDate(item.getRequestDate())
                .dueDate(item.getDueDate())
                .deletedAt(item.getDeletedAt())
                .commentCount(item.getCommentCount())
                .createdByName(item.getCreatedByName())
                .updatedByName(item.getUpdatedByName())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .properties(propertyResponses)
                .propertyValues(propertyValueMap.isEmpty() ? null : propertyValueMap)
                .sortOrder(item.getSortOrder())
                // 공유/이관 정보
                .isSharedToMe(item.getIsSharedToMe())
                .sharedByUsername(item.getSharedByUsername())
                .sharedByUserName(item.getSharedByUserName())
                .transferredFrom(item.getTransferredFrom())
                .transferredByUsername(item.getTransferredByUsername())
                .transferredByUserName(item.getTransferredByUserName())
                // 배정 정보 (v2.1)
                .isAssignedToMe(item.getIsAssignedToMe())
                .assignedByUsername(item.getAssignedByUsername())
                .assignedByUserName(item.getAssignedByUserName())
                .assignedAt(item.getAssignedAt())
                // 배정 대상 정보 (소유자 화면용)
                .assignedToUsername(item.getAssignedToUsername())
                .assignedToUserName(item.getAssignedToUserName())
                .createdBy(item.getCreatedBy())
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
        private String ownerType;     // 속성 소유자 타입 (GLOBAL, MANAGER, USER)
        private Integer sortOrder;    // 정렬 순서
        private String dataSourceType;  // 데이터 소스 타입 (INTERNAL, EXTERNAL)
        private Long externalQueryId;   // 외부 쿼리 ID

        public static PropertyValueResponse from(ItemProperty prop) {
            if (prop == null) {
                return null;
            }

            String displayValue = null;
            if (prop.getOptionName() != null) {
                displayValue = prop.getOptionName();
            } else if (prop.getValueUsername() != null) {
                displayValue = prop.getValueUsername();
            }

            return PropertyValueResponse.builder()
                    .propertyId(prop.getPropertyId())
                    .propertyName(prop.getPropertyName())
                    .propertyType(prop.getPropertyType())
                    .value(prop.getValue())
                    .displayValue(displayValue)
                    .color(prop.getOptionColor())
                    .ownerType(prop.getOwnerType())
                    .sortOrder(prop.getSortOrder())
                    .dataSourceType(prop.getDataSourceType() != null ? prop.getDataSourceType() : "INTERNAL")
                    .externalQueryId(prop.getExternalQueryId())
                    .build();
        }
    }

    // =============================================
    // 명시적 Getter (Lombok 호환성 - UserName 패턴)
    // =============================================

    public String getAssignedByUserName() {
        return assignedByUserName;
    }

    public String getAssignedToUserName() {
        return assignedToUserName;
    }

    public String getSharedByUserName() {
        return sharedByUserName;
    }

    public String getTransferredByUserName() {
        return transferredByUserName;
    }
}
