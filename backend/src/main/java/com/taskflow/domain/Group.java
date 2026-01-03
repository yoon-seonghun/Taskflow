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
 * USERNAME 기반 FK 참조 시스템:
 * - CREATED_BY, UPDATED_BY: USERNAME 참조
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
