package com.taskflow.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 보드-카테고리 매핑 엔티티
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BoardCategory {

    /**
     * 매핑 ID
     */
    private Long boardCategoryId;

    /**
     * 보드 ID
     */
    private Long boardId;

    /**
     * 카테고리 ID
     */
    private Long categoryId;

    /**
     * 정렬 순서
     */
    private Integer sortOrder;

    /**
     * 기본 카테고리 여부 (Y/N)
     */
    private String defaultYn;

    /**
     * 생성일시
     */
    private LocalDateTime createdAt;

    /**
     * 생성자
     */
    private String createdBy;

    /**
     * 삭제일시 (논리삭제)
     */
    private LocalDateTime deletedAt;

    /**
     * 삭제자 USERNAME
     */
    private String deletedBy;

    // ============================================
    // 조회용 필드
    // ============================================

    /**
     * 카테고리명
     */
    private String categoryName;

    /**
     * 카테고리 색상
     */
    private String categoryColor;

    /**
     * 카테고리 객체 (상세 조회용)
     */
    private Category category;
}
