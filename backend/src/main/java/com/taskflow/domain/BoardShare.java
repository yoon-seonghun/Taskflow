package com.taskflow.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 보드 공유 엔티티
 *
 * 테이블: TB_BOARD_SHARE
 *
 * USERNAME 기반 FK 참조 시스템:
 * - USERNAME: 공유받은 사용자 USERNAME 참조
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardShare {

    /**
     * 보드 공유 ID (PK)
     */
    private Long boardShareId;

    /**
     * 보드 ID (FK)
     */
    private Long boardId;

    /**
     * 공유받은 사용자 USERNAME (FK → TB_USER.USERNAME)
     */
    private String username;

    /**
     * 권한 레벨 (VIEW/EDIT/FULL)
     */
    private String permission;

    /**
     * 정렬 순서 (사용자별)
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
     * 보드명
     */
    private String boardName;

    /**
     * 사용자명
     */
    private String userName;

    /**
     * 부서명
     */
    private String departmentName;

    /**
     * 로그인 ID (Mapper JOIN 필드)
     */
    private String loginId;

    // =============================================
    // 상수 (VIEW/EDIT/FULL 권한 체계)
    // =============================================

    public static final String PERMISSION_VIEW = "VIEW";
    public static final String PERMISSION_EDIT = "EDIT";
    public static final String PERMISSION_FULL = "FULL";
    /** @deprecated Use PERMISSION_EDIT instead. 하위 호환성을 위해 유지 */
    public static final String PERMISSION_MEMBER = PERMISSION_EDIT;

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
     * 보드공유ID getter (Lombok 호환성)
     */
    public Long getBoardShareId() {
        return boardShareId;
    }

    /**
     * 보드ID getter (Lombok 호환성)
     */
    public Long getBoardId() {
        return boardId;
    }

    /**
     * 보드명 getter (Lombok 호환성)
     */
    public String getBoardName() {
        return boardName;
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
     * 생성일시 getter (Lombok 호환성)
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * 정렬순서 getter (Lombok 호환성)
     */
    public Integer getSortOrder() {
        return sortOrder;
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
}
