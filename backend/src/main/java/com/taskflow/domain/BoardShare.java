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
 * 보드를 다른 사용자에게 공유
 * - 소유자 외에 보드에 접근 가능한 사용자 관리
 * - 권한 레벨 지정 가능 (MEMBER: 편집, VIEWER: 조회만)
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
     * 공유받은 사용자 ID (FK)
     */
    private Long userId;

    /**
     * 권한 레벨 (MEMBER: 편집, VIEWER: 조회만)
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
     * 로그인 ID
     */
    private String loginId;

    /**
     * 부서명
     */
    private String departmentName;

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
     * 권한 레벨 비교 (FULL > EDIT > VIEW)
     */
    public int getPermissionLevel() {
        if (PERMISSION_FULL.equals(permission)) return 3;
        if (PERMISSION_EDIT.equals(permission)) return 2;
        if (PERMISSION_VIEW.equals(permission)) return 1;
        return 0;
    }
}
