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
 * 개별 업무를 다른 사용자에게 공유
 * - 보드 접근 권한 없이 특정 업무만 공유 가능
 * - 권한 레벨 지정 가능 (VIEW/EDIT/FULL)
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
     * 공유받은 사용자 ID (FK)
     */
    private Long userId;

    /**
     * 권한 (VIEW/EDIT/FULL)
     */
    private String permission;

    /**
     * 생성일시
     */
    private LocalDateTime createdAt;

    /**
     * 생성자 ID
     */
    private Long createdBy;

    /**
     * 수정일시
     */
    private LocalDateTime updatedAt;

    /**
     * 수정자 ID
     */
    private Long updatedBy;

    // =============================================
    // 추가 필드 (Mapper에서 JOIN으로 설정)
    // =============================================

    /**
     * 사용자 로그인 ID
     */
    private String loginId;

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

    // =============================================
    // 상수
    // =============================================

    public static final String PERMISSION_VIEW = "VIEW";
    public static final String PERMISSION_EDIT = "EDIT";
    public static final String PERMISSION_FULL = "FULL";

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
     * 권한 레벨 비교 (FULL > EDIT > VIEW)
     */
    public int getPermissionLevel() {
        if (PERMISSION_FULL.equals(permission)) return 3;
        if (PERMISSION_EDIT.equals(permission)) return 2;
        if (PERMISSION_VIEW.equals(permission)) return 1;
        return 0;
    }
}
