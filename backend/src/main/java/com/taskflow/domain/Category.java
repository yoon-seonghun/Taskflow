package com.taskflow.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 카테고리 엔티티 (전역)
 *
 * 테이블: TB_CATEGORY
 *
 * 모든 보드에서 공통으로 사용하는 전역 카테고리
 * USERNAME 기반 FK 참조 시스템:
 * - CREATED_BY, UPDATED_BY: USERNAME 참조
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Category {

    /**
     * 카테고리 ID (PK)
     */
    private Long categoryId;

    /**
     * 카테고리명
     */
    private String categoryName;

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
    // 편의 메서드
    // =============================================

    /**
     * 활성 카테고리 여부
     */
    public boolean isActive() {
        return "Y".equals(useYn);
    }
}
