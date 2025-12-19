package com.taskflow.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 그룹 엔티티
 *
 * 테이블: TB_GROUP
 *
 * 그룹 vs 부서 차이:
 * - 부서: 조직도 기반의 고정 소속 (1인 1부서)
 * - 그룹: 프로젝트/업무 기반의 유연한 팀 (1인 다중 그룹 가능)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Group {

    /**
     * 그룹 ID (PK)
     */
    private Long groupId;

    /**
     * 그룹 코드 (UNIQUE)
     */
    private String groupCode;

    /**
     * 그룹명
     */
    private String groupName;

    /**
     * 그룹 설명
     */
    private String description;

    /**
     * 표시 색상 (#RRGGBB)
     */
    private String color;

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
    // 추가 필드 (Mapper에서 설정)
    // =============================================

    /**
     * 멤버 수
     */
    private Integer memberCount;

    // =============================================
    // 편의 메서드
    // =============================================

    /**
     * 활성 그룹 여부
     */
    public boolean isActive() {
        return "Y".equals(useYn);
    }
}
