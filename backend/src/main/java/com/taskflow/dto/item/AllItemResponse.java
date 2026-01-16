package com.taskflow.dto.item;

import com.taskflow.domain.Item;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 전체 업무 응답 DTO (v2.1)
 * - ItemResponse 필드 + 출처 정보 (sourceType)
 */
@Getter
@Builder
public class AllItemResponse {

    // =============================================
    // 기본 필드 (ItemResponse와 동일)
    // =============================================

    private Long itemId;
    private Long boardId;
    private String boardName;
    private String boardColor;
    private String ownerUsername;
    private String ownerName;
    private Long groupId;
    private String groupName;
    private String groupColor;
    private Long categoryId;
    private String categoryName;
    private String categoryColor;

    // 하위 업무 관련 필드
    private Long parentItemId;
    private Integer itemDepth;
    private Integer childSortOrder;
    private Integer childCount;
    private Integer completedChildCount;
    private Boolean hasChildren;
    private Boolean canCreateChild;
    private ParentInfo parentInfo;
    private ParentInfo rootInfo;
    private List<AllItemResponse> children;

    // 기본 업무 필드
    private String title;
    private String content;
    private String description;
    private String status;
    private String priority;
    private String assigneeUsername;
    private String assigneeName;
    private LocalDate requestDate;
    private LocalDate dueDate;
    private Integer sortOrder;
    private LocalDateTime deletedAt;
    private Integer commentCount;
    private Integer shareCount;
    private String createdByName;
    private String updatedByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;

    // 동적 속성
    private List<ItemResponse.PropertyValueResponse> properties;
    private Map<Long, Object> propertyValues;

    // =============================================
    // 출처 관련 필드 (v2.1 신규)
    // =============================================

    /**
     * 업무 출처 타입
     * - OWNED: 내 보드 업무
     * - BOARD_SHARED: 공유받은 보드의 업무
     * - ITEM_SHARED: 개별 공유받은 업무
     * - ASSIGNED: 배당받은 업무
     */
    private String sourceType;

    /**
     * 공유자 USERNAME
     */
    private String sharedByUsername;

    /**
     * 공유자 이름
     */
    private String sharedByName;

    /**
     * 공유 일시
     */
    private LocalDateTime sharedAt;

    // =============================================
    // 배당 관련 필드 (v2.1)
    // =============================================

    /**
     * 공유 유형 (SHARE/ASSIGN)
     */
    private String shareType;

    /**
     * 권한 (VIEW/EDIT/FULL)
     */
    private String permission;

    /**
     * 배당자 USERNAME
     */
    private String assignedByUsername;

    /**
     * 배당자 이름
     */
    private String assignedByName;

    /**
     * 배당 일시
     */
    private LocalDateTime assignedAt;

    // =============================================
    // 기타 공유/이관 정보
    // =============================================

    private Boolean isSharedToMe;
    private Boolean isAssignedToMe;
    private Long transferredFrom;
    private String transferredByUsername;
    private String transferredByUserName;

    // =============================================
    // 접근 권한 정보
    // =============================================

    private ItemAccessInfo accessInfo;

    /**
     * Item 도메인 객체를 AllItemResponse로 변환
     */
    public static AllItemResponse from(Item item) {
        if (item == null) {
            return null;
        }

        return AllItemResponse.builder()
                .itemId(item.getItemId())
                .boardId(item.getBoardId())
                .boardName(item.getBoardName())
                .boardColor(item.getBoardColor())
                .ownerUsername(item.getOwnerUsername())
                .ownerName(item.getOwnerName())
                .groupId(item.getGroupId())
                .groupName(item.getGroupName())
                .groupColor(item.getGroupColor())
                .categoryId(item.getCategoryId())
                .categoryName(item.getCategoryName())
                .categoryColor(item.getCategoryColor())
                // 하위 업무 정보
                .parentItemId(item.getParentItemId())
                .itemDepth(item.getItemDepth() != null ? item.getItemDepth() : 0)
                .childSortOrder(item.getChildSortOrder())
                .childCount(item.getChildCount())
                .completedChildCount(item.getCompletedChildCount())
                .hasChildren(item.getHasChildren())
                .canCreateChild(item.getCanCreateChild())
                .parentInfo(buildParentInfo(item))
                .rootInfo(buildRootInfo(item))
                // 기본 업무 정보
                .title(item.getTitle())
                .content(item.getContent())
                .description(item.getDescription())
                .status(item.getStatus())
                .priority(item.getPriority())
                .assigneeUsername(item.getAssigneeUsername())
                .assigneeName(item.getAssigneeName())
                .requestDate(item.getRequestDate())
                .dueDate(item.getDueDate())
                .sortOrder(item.getSortOrder())
                .deletedAt(item.getDeletedAt())
                .commentCount(item.getCommentCount())
                .shareCount(item.getShareCount())
                .createdByName(item.getCreatedByName())
                .updatedByName(item.getUpdatedByName())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .createdBy(item.getCreatedBy())
                // 출처 정보
                .sourceType(item.getSourceType())
                .sharedByUsername(item.getSharedByUsername())
                .sharedByName(item.getSharedByUserName())
                .sharedAt(item.getSharedAt())
                // 배당 정보
                .shareType(item.getShareType())
                .permission(item.getPermission())
                .assignedByUsername(item.getAssignedByUsername())
                .assignedByName(item.getAssignedByUserName())
                .assignedAt(item.getAssignedAt())
                // 기타 공유/이관 정보
                .isSharedToMe(item.getIsSharedToMe())
                .isAssignedToMe(item.getIsAssignedToMe())
                .transferredFrom(item.getTransferredFrom())
                .transferredByUsername(item.getTransferredByUsername())
                .transferredByUserName(item.getTransferredByUserName())
                .build();
    }

    /**
     * 부모 업무 정보 빌드
     */
    private static ParentInfo buildParentInfo(Item item) {
        if (item.getParentItemId() == null) {
            return null;
        }
        return ParentInfo.builder()
                .itemId(item.getParentItemId())
                .title(item.getParentTitle())
                .status(item.getParentStatus())
                .build();
    }

    /**
     * 최상위 업무 정보 빌드
     */
    private static ParentInfo buildRootInfo(Item item) {
        if (item.getRootItemId() == null || item.getParentItemId() == null) {
            return null;
        }
        return ParentInfo.builder()
                .itemId(item.getRootItemId())
                .title(item.getRootTitle())
                .status(item.getRootStatus())
                .build();
    }

    /**
     * 리스트 변환
     */
    public static List<AllItemResponse> fromList(List<Item> items) {
        if (items == null) {
            return List.of();
        }
        return items.stream()
                .map(AllItemResponse::from)
                .toList();
    }

    // =============================================
    // 내부 클래스: 부모/루트 업무 정보
    // =============================================

    @Getter
    @Builder
    public static class ParentInfo {
        private Long itemId;
        private String title;
        private String status;
    }

    // =============================================
    // 명시적 Getter (Lombok 호환성)
    // =============================================

    public String getAssignedByName() {
        return assignedByName;
    }

    public String getSharedByName() {
        return sharedByName;
    }

    public String getTransferredByUserName() {
        return transferredByUserName;
    }
}
