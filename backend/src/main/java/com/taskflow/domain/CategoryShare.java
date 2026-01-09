package com.taskflow.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 카테고리 공유 엔티티
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryShare {

    /**
     * 공유 ID
     */
    private Long shareId;

    /**
     * 카테고리 ID
     */
    private Long categoryId;

    /**
     * 공유 대상 유형 (USER, DEPARTMENT)
     */
    private String shareType;

    /**
     * 공유 대상 (USERNAME 또는 DEPT_CODE)
     */
    private String shareTarget;

    /**
     * 권한 (READ, EDIT)
     */
    private String permission;

    /**
     * 생성일시
     */
    private LocalDateTime createdAt;

    /**
     * 생성자
     */
    private String createdBy;

    /**
     * 수정일시
     */
    private LocalDateTime updatedAt;

    /**
     * 수정자
     */
    private String updatedBy;

    // ============================================
    // 조회용 필드
    // ============================================

    /**
     * 공유 대상 이름 (사용자명 또는 부서명)
     */
    private String shareTargetName;

    // ============================================
    // 상수
    // ============================================

    public static final String SHARE_TYPE_USER = "USER";
    public static final String SHARE_TYPE_DEPARTMENT = "DEPARTMENT";

    public static final String PERMISSION_READ = "READ";
    public static final String PERMISSION_EDIT = "EDIT";
}
