package com.taskflow.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 카테고리 (속성 그룹) 엔티티
 *
 * 테이블: TB_CATEGORY
 *
 * v2.0 변경사항:
 * - categoryCode, description, ownerUsername 필드 추가
 * - 사용자별 카테고리 소유 구조
 * - 카테고리 공유 기능 지원
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
     * 카테고리 코드
     */
    private String categoryCode;

    /**
     * 카테고리명
     */
    private String categoryName;

    /**
     * 설명
     */
    private String description;

    /**
     * 표시 색상 (#RRGGBB)
     */
    private String categoryColor;

    /**
     * 소유자 USERNAME
     */
    private String ownerUsername;

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

    // ============================================
    // 연관 데이터 (조회용)
    // ============================================

    /**
     * 카테고리에 배정된 속성 목록
     */
    private List<CategoryProperty> properties;

    /**
     * 카테고리 공유 목록
     */
    private List<CategoryShare> shares;

    /**
     * 소유자 이름 (조회용)
     */
    private String ownerName;

    /**
     * 속성 개수 (조회용)
     */
    private Integer propertyCount;

    // =============================================
    // 하위 호환성을 위한 getter (color -> categoryColor)
    // =============================================

    /**
     * 색상 getter (하위 호환성)
     */
    public String getColor() {
        return categoryColor;
    }

    /**
     * 색상 setter (하위 호환성)
     */
    public void setColor(String color) {
        this.categoryColor = color;
    }

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
