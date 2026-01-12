package com.taskflow.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 보드-속성 매핑 엔티티
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardProperty {

    /**
     * 매핑 ID
     */
    private Long boardPropertyId;

    /**
     * 보드 ID
     */
    private Long boardId;

    /**
     * 속성 ID
     */
    private Long propertyId;

    /**
     * 필수 여부 (보드 레벨)
     */
    private String requiredYn;

    /**
     * 표시 여부
     */
    private String visibleYn;

    /**
     * 표시 순서
     */
    private Integer sortOrder;

    /**
     * 기본값 (카테고리에서 상속)
     */
    private String defaultValue;

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

    /**
     * 표시 여부 확인
     */
    public boolean isVisible() {
        return "Y".equals(visibleYn);
    }
}
