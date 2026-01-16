package com.taskflow.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 업무 아이템 엔티티
 *
 * 테이블: TB_ITEM
 *
 * USERNAME 기반 FK 참조 시스템:
 * - ASSIGNEE_USERNAME: 담당자 USERNAME 참조
 * - COMPLETED_BY, DELETED_BY, CREATED_BY, UPDATED_BY: USERNAME 참조
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Item {

    /**
     * 아이템 ID (PK)
     */
    private Long itemId;

    /**
     * 보드 ID (FK)
     */
    private Long boardId;

    /**
     * 현재 소유자 USERNAME (v2.2.1)
     * 생성 시: CREATED_BY와 동일
     * 이관 시: 이관 대상자로 변경
     */
    private String ownerUsername;

    /**
     * 그룹 ID (FK) - TB_GROUP 연동
     */
    private Long groupId;

    /**
     * 카테고리 ID (FK) - TB_PROPERTY_OPTION 연동
     */
    private Long categoryId;

    // =============================================
    // 하위 업무 관련 필드 (v2.2)
    // =============================================

    /**
     * 부모 업무 ID (NULL=기본 업무)
     */
    private Long parentItemId;

    /**
     * 업무 깊이 (0=기본, 1~2=하위)
     */
    private Integer itemDepth;

    /**
     * 하위 업무 내 정렬 순서
     */
    private Integer childSortOrder;

    /**
     * 아이템 제목 (DB: CONTENT)
     */
    private String title;

    /**
     * 제목 원본 (DB: CONTENT) - title과 동일, 호환성용
     */
    private String content;

    /**
     * 상세 내용 - 마크다운 (DB: DESCRIPTION)
     */
    private String description;

    /**
     * 상태 (NOT_STARTED, IN_PROGRESS, PENDING, COMPLETED, DELETED)
     */
    private String status;

    /**
     * 우선순위 (URGENT, HIGH, NORMAL, LOW)
     */
    private String priority;

    /**
     * 담당자 USERNAME (FK → TB_USER.USERNAME)
     */
    private String assigneeUsername;

    /**
     * 요청일 (DB: REQUEST_DATE)
     */
    private LocalDate requestDate;

    /**
     * 마감일 (DB: DUE_DATE)
     */
    private LocalDate dueDate;

    /**
     * 이전 상태 (삭제/복원 전 상태)
     */
    private String previousStatus;

    /**
     * 완료 처리자 USERNAME
     */
    private String completedBy;

    /**
     * 삭제일시
     */
    private LocalDateTime deletedAt;

    /**
     * 삭제자 USERNAME
     */
    private String deletedBy;

    /**
     * 이관 원본 보드 ID
     */
    private Long transferredFrom;

    /**
     * 이관 일시
     */
    private LocalDateTime transferredAt;

    /**
     * 정렬 순서
     */
    private Integer sortOrder;

    /**
     * 생성일시
     */
    private LocalDateTime createdAt;

    /**
     * 생성자 USERNAME
     */
    private String createdBy;

    /**
     * 수정일시
     */
    private LocalDateTime updatedAt;

    /**
     * 수정자 USERNAME
     */
    private String updatedBy;

    // =============================================
    // 추가 필드 (Mapper에서 JOIN으로 설정)
    // =============================================

    /**
     * 보드명
     */
    private String boardName;

    /**
     * 보드 색상 (조인)
     */
    private String boardColor;

    /**
     * 소유자 이름 (조인)
     */
    private String ownerName;

    /**
     * 그룹명
     */
    private String groupName;

    /**
     * 그룹 색상
     */
    private String groupColor;

    /**
     * 카테고리명 (조인)
     */
    private String categoryName;

    /**
     * 카테고리 색상 (조인)
     */
    private String categoryColor;

    /**
     * 담당자명 (조인)
     */
    private String assigneeName;

    /**
     * 생성자명 (조인)
     */
    private String createdByName;

    /**
     * 수정자명 (조인)
     */
    private String updatedByName;

    /**
     * 댓글 수
     */
    private Integer commentCount;

    /**
     * 공유 사용자 수 (v2.2.1)
     */
    private Integer shareCount;

    // =============================================
    // 공유/이관 정보 (Mapper에서 JOIN으로 설정)
    // =============================================

    /**
     * 업무 출처 타입 (전체 업무 조회용)
     * OWNED, BOARD_SHARED, ITEM_SHARED, ASSIGNED
     */
    private String sourceType;

    /**
     * 공유 유형 (SHARE/ASSIGN)
     */
    private String shareType;

    /**
     * 공유/배당 권한 (VIEW/EDIT/FULL)
     */
    private String permission;

    /**
     * 공유 일시
     */
    private LocalDateTime sharedAt;

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
     * 이관해준 사용자 USERNAME (조인)
     */
    private String transferredByUsername;

    /**
     * 이관해준 사용자 이름 (조인)
     */
    private String transferredByUserName;

    // =============================================
    // 배정 정보 (v2.1 - Mapper에서 JOIN으로 설정)
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
     * 동적 속성값 목록
     */
    @Builder.Default
    private List<ItemProperty> properties = new ArrayList<>();

    /**
     * 동적 속성값 맵 (propertyId -> value)
     */
    @Builder.Default
    private Map<Long, Object> propertyValues = new HashMap<>();

    // =============================================
    // 하위 업무 조인/계산 필드 (v2.2)
    // =============================================

    /**
     * 하위 업무 수
     */
    private Integer childCount;

    /**
     * 완료된 하위 업무 수
     */
    private Integer completedChildCount;

    /**
     * 하위 업무 존재 여부
     */
    private Boolean hasChildren;

    /**
     * 하위 업무 생성 가능 여부 (depth < 2)
     */
    private Boolean canCreateChild;

    /**
     * 부모 업무 제목 (조인)
     */
    private String parentTitle;

    /**
     * 부모 업무 상태 (조인)
     */
    private String parentStatus;

    /**
     * 최상위 업무 ID (조인)
     */
    private Long rootItemId;

    /**
     * 최상위 업무 제목 (조인)
     */
    private String rootTitle;

    /**
     * 최상위 업무 상태 (조인)
     */
    private String rootStatus;

    /**
     * 하위 업무 목록 (트리 조회용)
     */
    @Builder.Default
    private List<Item> children = new ArrayList<>();

    // =============================================
    // 상수: 상태
    // =============================================

    public static final String STATUS_NOT_STARTED = "NOT_STARTED";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_DELETED = "DELETED";
    public static final String STATUS_PENDING = "PENDING";

    // =============================================
    // 상수: 우선순위
    // =============================================

    public static final String PRIORITY_URGENT = "URGENT";
    public static final String PRIORITY_HIGH = "HIGH";
    public static final String PRIORITY_NORMAL = "NORMAL";
    public static final String PRIORITY_LOW = "LOW";

    // =============================================
    // 편의 메서드
    // =============================================

    /**
     * 완료 상태 여부
     */
    public boolean isCompleted() {
        return STATUS_COMPLETED.equals(status);
    }

    /**
     * 삭제 상태 여부
     */
    public boolean isDeleted() {
        return STATUS_DELETED.equals(status);
    }

    /**
     * 진행 중 여부
     */
    public boolean isInProgress() {
        return STATUS_IN_PROGRESS.equals(status);
    }

    /**
     * 시작 전 여부
     */
    public boolean isNotStarted() {
        return STATUS_NOT_STARTED.equals(status);
    }

    /**
     * 활성 아이템 여부 (완료/삭제 아님)
     */
    public boolean isActive() {
        return !isCompleted() && !isDeleted();
    }

    /**
     * 속성값 추가
     */
    public void addProperty(ItemProperty property) {
        if (properties == null) {
            properties = new ArrayList<>();
        }
        properties.add(property);
    }

    /**
     * 속성값 맵에 추가
     */
    public void putPropertyValue(Long propertyId, Object value) {
        if (propertyValues == null) {
            propertyValues = new HashMap<>();
        }
        propertyValues.put(propertyId, value);
    }

    // =============================================
    // 하위 업무 편의 메서드 (v2.2)
    // =============================================

    /**
     * 기본 업무 여부 (depth = 0)
     */
    public boolean isRootItem() {
        return itemDepth == null || itemDepth == 0;
    }

    /**
     * 하위 업무 여부 (depth > 0)
     */
    public boolean isChildItem() {
        return itemDepth != null && itemDepth > 0;
    }

    /**
     * 하위 업무 생성 가능 여부
     */
    public boolean canCreateChild() {
        return itemDepth == null || itemDepth < 2;
    }

    /**
     * 하위 업무 추가
     */
    public void addChild(Item child) {
        if (children == null) {
            children = new ArrayList<>();
        }
        children.add(child);
    }

    /**
     * 하위 업무 존재 여부
     */
    public boolean hasChildren() {
        return hasChildren != null && hasChildren;
    }

    /**
     * 미완료 하위 업무 수
     */
    public int getIncompleteChildCount() {
        if (childCount == null || completedChildCount == null) {
            return 0;
        }
        return childCount - completedChildCount;
    }

    // =============================================
    // 명시적 Getter (Lombok 빌드 호환성)
    // =============================================

    /**
     * 공유해준 사용자 USERNAME getter
     */
    public String getSharedByUsername() {
        return sharedByUsername;
    }

    /**
     * 공유해준 사용자 이름 getter
     */
    public String getSharedByUserName() {
        return sharedByUserName;
    }

    /**
     * 이관해준 사용자 USERNAME getter
     */
    public String getTransferredByUsername() {
        return transferredByUsername;
    }

    /**
     * 이관해준 사용자 이름 getter
     */
    public String getTransferredByUserName() {
        return transferredByUserName;
    }

    /**
     * 공유받은 업무 여부 getter
     */
    public Boolean getIsSharedToMe() {
        return isSharedToMe;
    }

    /**
     * 배정받은 업무 여부 getter
     */
    public Boolean getIsAssignedToMe() {
        return isAssignedToMe;
    }

    /**
     * 배정해준 사용자 USERNAME getter
     */
    public String getAssignedByUsername() {
        return assignedByUsername;
    }

    /**
     * 배정해준 사용자 이름 getter
     */
    public String getAssignedByUserName() {
        return assignedByUserName;
    }

    /**
     * 배정 대상 사용자 USERNAME getter
     */
    public String getAssignedToUsername() {
        return assignedToUsername;
    }

    /**
     * 배정 대상 사용자 이름 getter
     */
    public String getAssignedToUserName() {
        return assignedToUserName;
    }

    /**
     * 정렬 순서 getter
     */
    public Integer getSortOrder() {
        return sortOrder;
    }

    /**
     * 정렬 순서 setter
     */
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    /**
     * 배정해준 사용자 이름 setter
     */
    public void setAssignedByUserName(String assignedByUserName) {
        this.assignedByUserName = assignedByUserName;
    }

    /**
     * 공유해준 사용자 이름 setter
     */
    public void setSharedByUserName(String sharedByUserName) {
        this.sharedByUserName = sharedByUserName;
    }

    /**
     * 소유자 USERNAME getter
     */
    public String getOwnerUsername() {
        return ownerUsername;
    }

    /**
     * 소유자 USERNAME setter
     */
    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }

    /**
     * 소유자 이름 getter
     */
    public String getOwnerName() {
        return ownerName;
    }

    /**
     * 소유자 이름 setter
     */
    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    // =============================================
    // 전체 업무 조회용 필드 getter/setter (v2.1)
    // =============================================

    /**
     * 출처 타입 getter
     */
    public String getSourceType() {
        return sourceType;
    }

    /**
     * 출처 타입 setter
     */
    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    /**
     * 공유 유형 getter
     */
    public String getShareType() {
        return shareType;
    }

    /**
     * 공유 유형 setter
     */
    public void setShareType(String shareType) {
        this.shareType = shareType;
    }

    /**
     * 권한 getter
     */
    public String getPermission() {
        return permission;
    }

    /**
     * 권한 setter
     */
    public void setPermission(String permission) {
        this.permission = permission;
    }

    /**
     * 공유 일시 getter
     */
    public LocalDateTime getSharedAt() {
        return sharedAt;
    }

    /**
     * 공유 일시 setter
     */
    public void setSharedAt(LocalDateTime sharedAt) {
        this.sharedAt = sharedAt;
    }

    /**
     * 보드 색상 getter
     */
    public String getBoardColor() {
        return boardColor;
    }

    /**
     * 보드 색상 setter
     */
    public void setBoardColor(String boardColor) {
        this.boardColor = boardColor;
    }
}
