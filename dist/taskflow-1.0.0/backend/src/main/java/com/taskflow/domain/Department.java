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
 * 계층형 구조 지원 (자기참조 FK)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Department {

    /**
     * 부서 ID (PK)
     */
    private Long departmentId;

    /**
     * 부서 코드 (UNIQUE)
     */
    private String departmentCode;

    /**
     * 부서명
     */
    private String departmentName;

    /**
     * 상위 부서 ID (FK, NULL = 최상위)
     */
    private Long parentId;

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
    // 계층 구조 필드 (Mapper/Service에서 설정)
    // =============================================

    /**
     * 계층 레벨 (0 = 최상위)
     */
    private Integer level;

    /**
     * 경로 (예: "1/2/3")
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
        return parentId == null;
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
