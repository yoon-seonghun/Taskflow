package com.taskflow.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 부서 엔티티
 *
 * 테이블: TB_DEPARTMENT
 *
 * DEPARTMENT_CODE 기반 FK 참조 시스템:
 * - DEPARTMENT_ID: 내부 식별자
 * - DEPARTMENT_CODE: FK 참조 키 (UNIQUE)
 * - PARENT_CODE: 상위 부서 코드 참조
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Department {

    /**
     * 부서 ID (PK) - 내부 식별자
     */
    private Long departmentId;

    /**
     * 부서 코드 (UNIQUE) - FK 참조 키
     */
    private String departmentCode;

    /**
     * 부서명
     */
    private String departmentName;

    /**
     * 상위 부서 코드 (FK → TB_DEPARTMENT.DEPARTMENT_CODE, NULL = 최상위)
     */
    private String parentCode;

    /**
     * 정렬 순서
     */
    private Integer sortOrder;

    /**
     * 사용 여부 (Y/N)
     */
    private String useYn;

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

    /**
     * 마지막 동기화 일시 (External 모드)
     */
    private LocalDateTime lastSyncedAt;

    // =============================================
    // 계층 구조 필드 (Mapper/Service에서 설정)
    // =============================================

    /**
     * 계층 레벨 (0 = 최상위)
     */
    private Integer level;

    /**
     * 경로 (예: "ROOT/DEV/DEV1")
     */
    private String path;

    /**
     * 상위 부서명
     */
    private String parentName;

    /**
     * 하위 부서 목록 (트리 구조용)
     */
    @Builder.Default
    private List<Department> children = new ArrayList<>();

    /**
     * 하위 부서 수
     */
    private Integer childCount;

    /**
     * 소속 사용자 수
     */
    private Integer userCount;

    // =============================================
    // 편의 메서드
    // =============================================

    /**
     * 활성 부서 여부
     */
    public boolean isActive() {
        return "Y".equals(useYn);
    }

    /**
     * 최상위 부서 여부
     */
    public boolean isRoot() {
        return parentCode == null;
    }

    /**
     * 하위 부서 존재 여부
     */
    public boolean hasChildren() {
        return children != null && !children.isEmpty();
    }

    /**
     * 하위 부서 추가
     */
    public void addChild(Department child) {
        if (this.children == null) {
            this.children = new ArrayList<>();
        }
        this.children.add(child);
    }
}
