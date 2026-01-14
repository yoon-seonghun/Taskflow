package com.taskflow.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 업무 공유 엔티티
 *
 * 테이블: TB_ITEM_SHARE
 *
 * USERNAME 기반 FK 참조 시스템:
 * - USERNAME: 공유받은 사용자 USERNAME 참조
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemShare {

    /**
     * 업무 공유 ID (PK)
     */
    private Long itemShareId;

    /**
     * 업무 ID (FK)
     */
    private Long itemId;

    /**
     * 공유받은 사용자 USERNAME (FK → TB_USER.USERNAME)
     */
    private String username;

    /**
     * 공유 유형 (SHARE: 공유, ASSIGN: 배당)
     */
    private String shareType;

    /**
     * 권한 (VIEW/EDIT/FULL)
     */
    private String permission;

    /**
     * 배당자 USERNAME (ASSIGN일 경우)
     */
    private String assignedBy;

    /**
     * 배당 일시
     */
    private LocalDateTime assignedAt;

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

    /**
     * 삭제일시 (논리삭제)
     */
    private LocalDateTime deletedAt;

    /**
     * 삭제자 USERNAME
     */
    private String deletedBy;

    // =============================================
    // 추가 필드 (Mapper에서 JOIN으로 설정)
    // =============================================

    /**
     * 사용자명
     */
    private String userName;

    /**
     * 부서명
     */
    private String departmentName;

    /**
     * 업무 제목
     */
    private String itemContent;

    /**
     * 로그인 ID (Mapper JOIN 필드)
     */
    private String loginId;

    /**
     * 배당자 이름 (Mapper JOIN 필드)
     */
    private String assignedByName;

    // =============================================
    // 상수
    // =============================================

    public static final String PERMISSION_VIEW = "VIEW";
    public static final String PERMISSION_EDIT = "EDIT";
    public static final String PERMISSION_FULL = "FULL";

    public static final String SHARE_TYPE_SHARE = "SHARE";
    public static final String SHARE_TYPE_ASSIGN = "ASSIGN";

    // =============================================
    // 편의 메서드
    // =============================================

    /**
     * 조회 권한 여부
     */
    public boolean canView() {
        return permission != null;
    }

    /**
     * 수정 권한 여부
     */
    public boolean canEdit() {
        return PERMISSION_EDIT.equals(permission) || PERMISSION_FULL.equals(permission);
    }

    /**
     * 삭제 권한 여부
     */
    public boolean canDelete() {
        return PERMISSION_FULL.equals(permission);
    }

    /**
     * 사용자명 getter (Lombok 호환성)
     */
    public String getUserName() {
        return userName;
    }

    /**
     * USERNAME getter (Lombok 호환성)
     */
    public String getUsername() {
        return username;
    }

    /**
     * 부서명 getter (Lombok 호환성)
     */
    public String getDepartmentName() {
        return departmentName;
    }

    /**
     * 권한 getter (Lombok 호환성)
     */
    public String getPermission() {
        return permission;
    }

    /**
     * 아이템ID getter (Lombok 호환성)
     */
    public Long getItemId() {
        return itemId;
    }

    /**
     * 아이템공유ID getter (Lombok 호환성)
     */
    public Long getItemShareId() {
        return itemShareId;
    }

    /**
     * 생성일시 getter (Lombok 호환성)
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 수정일시 getter (Lombok 호환성)
     */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    /**
     * 권한 레벨 비교 (FULL > EDIT > VIEW)
     */
    public int getPermissionLevel() {
        if (PERMISSION_FULL.equals(permission)) return 3;
        if (PERMISSION_EDIT.equals(permission)) return 2;
        if (PERMISSION_VIEW.equals(permission)) return 1;
        return 0;
    }

    /**
     * 배당 여부 확인
     */
    public boolean isAssignment() {
        return SHARE_TYPE_ASSIGN.equals(shareType);
    }

    /**
     * 공유 여부 확인
     */
    public boolean isShare() {
        return SHARE_TYPE_SHARE.equals(shareType) || shareType == null;
    }

    /**
     * 공유 유형 getter
     */
    public String getShareType() {
        return shareType;
    }

    /**
     * 배당자 getter
     */
    public String getAssignedBy() {
        return assignedBy;
    }

    /**
     * 배당자 이름 getter
     */
    public String getAssignedByName() {
        return assignedByName;
    }

    /**
     * 배당 일시 getter
     */
    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }
}
