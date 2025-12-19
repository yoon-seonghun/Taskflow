package com.taskflow.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 아이템 다중선택 속성값 엔티티
 *
 * 테이블: TB_ITEM_PROPERTY_MULTI
 *
 * MULTI_SELECT 타입 속성의 개별 선택값을 저장
 * - 정규화된 다중 선택 값 관리
 * - TB_ITEM_PROPERTY.VALUE_TEXT에는 콤마 구분 ID 저장 (검색용)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemPropertyMulti {

    /**
     * 다중선택 속성값 ID (PK)
     */
    private Long itemPropertyMultiId;

    /**
     * 아이템 ID (FK)
     */
    private Long itemId;

    /**
     * 속성 정의 ID (FK)
     */
    private Long propertyId;

    /**
     * 옵션 ID (FK)
     */
    private Long optionId;

    /**
     * 생성일시
     */
    private LocalDateTime createdAt;

    /**
     * 생성자 ID
     */
    private Long createdBy;

    // =============================================
    // 추가 필드 (Mapper에서 JOIN으로 설정)
    // =============================================

    /**
     * 옵션명
     */
    private String optionName;

    /**
     * 옵션 색상
     */
    private String optionColor;

    /**
     * 속성명
     */
    private String propertyName;
}
