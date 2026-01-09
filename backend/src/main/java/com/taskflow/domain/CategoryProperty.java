package com.taskflow.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 카테고리-속성 매핑 엔티티
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryProperty {

    /**
     * 매핑 ID
     */
    private Long categoryPropertyId;

    /**
     * 카테고리 ID
     */
    private Long categoryId;

    /**
     * 속성 ID
     */
    private Long propertyId;

    /**
     * 속성 표시 순서
     */
    private Integer sortOrder;

    /**
     * 기본값
     */
    private String defaultValue;

    /**
     * 필수 여부 (카테고리 레벨) - v2.0
     */
    private String requiredYn;

    /**
     * 생성일시
     */
    private LocalDateTime createdAt;

    /**
     * 생성자
     */
    private String createdBy;

    // ============================================
    // 조회용 필드 (속성 정보)
    // ============================================

    /**
     * 속성명
     */
    private String propertyName;

    /**
     * 속성 타입
     */
    private String propertyType;

    /**
     * 속성 소유 유형
     */
    private String ownerType;

    /**
     * 속성 정의 객체 (상세 조회용)
     */
    private PropertyDef propertyDef;

    // ============================================
    // 편의 메서드
    // ============================================

    /**
     * 필수 여부 확인
     */
    public boolean isRequired() {
        return "Y".equals(requiredYn);
    }
}
