package com.taskflow.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Todo 공유 엔티티
 *
 * 테이블: TB_TODO_SHARE
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodoShare {

    /**
     * 공유 ID (PK)
     */
    private Long shareId;

    /**
     * Todo ID (FK → TB_TODO)
     */
    private Long todoId;

    /**
     * 공유받은 사용자 ID (FK → TB_USER)
     */
    private Long sharedUserId;

    /**
     * 공유해준 사용자 ID (FK → TB_USER)
     */
    private Long sharedByUserId;

    /**
     * 권한 (VIEW/EDIT)
     */
    private String permission;

    /**
     * 공유일시
     */
    private LocalDateTime createdAt;

    /**
     * 생성자 USERNAME
     */
    private String createdBy;

    // =============================================
    // 추가 필드 (Mapper에서 JOIN으로 설정)
    // =============================================

    /**
     * 공유받은 사용자명
     */
    private String sharedUserName;

    /**
     * 공유받은 사용자 로그인ID
     */
    private String sharedUsername;

    /**
     * 공유해준 사용자명
     */
    private String sharedByUserName;

    /**
     * 공유해준 사용자 로그인ID
     */
    private String sharedByUsername;

    /**
     * Todo 제목
     */
    private String todoTitle;

    /**
     * 공유받은 사용자 부서명
     */
    private String departmentName;

    /**
     * Todo 마감일
     */
    private LocalDate todoDueDate;

    /**
     * Todo 마감시간
     */
    private LocalTime todoDueTime;

    /**
     * Todo 우선순위
     */
    private String todoPriority;

    /**
     * Todo 소유자명
     */
    private String todoOwnerUserName;

    // =============================================
    // 상수
    // =============================================

    public static final String PERMISSION_VIEW = "VIEW";
    public static final String PERMISSION_EDIT = "EDIT";

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
        return PERMISSION_EDIT.equals(permission);
    }

    /**
     * 권한 레벨 비교 (EDIT > VIEW)
     */
    public int getPermissionLevel() {
        if (PERMISSION_EDIT.equals(permission)) return 2;
        if (PERMISSION_VIEW.equals(permission)) return 1;
        return 0;
    }

    // Lombok 호환성 getters
    public Long getShareId() {
        return shareId;
    }

    public Long getTodoId() {
        return todoId;
    }

    public Long getSharedUserId() {
        return sharedUserId;
    }

    public Long getSharedByUserId() {
        return sharedByUserId;
    }

    public String getPermission() {
        return permission;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getSharedUserName() {
        return sharedUserName;
    }

    public String getSharedUsername() {
        return sharedUsername;
    }

    public String getSharedByUserName() {
        return sharedByUserName;
    }

    public String getSharedByUsername() {
        return sharedByUsername;
    }

    public String getTodoTitle() {
        return todoTitle;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public LocalDate getTodoDueDate() {
        return todoDueDate;
    }

    public LocalTime getTodoDueTime() {
        return todoDueTime;
    }

    public String getTodoPriority() {
        return todoPriority;
    }

    public String getTodoOwnerUserName() {
        return todoOwnerUserName;
    }
}
