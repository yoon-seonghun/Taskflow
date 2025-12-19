package com.taskflow.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 아이템 속성값 엔티티
 *
 * 테이블: TB_ITEM_PROPERTY
 *
 * 아이템별 동적 속성의 값을 저장 (EAV 패턴)
 * - 속성 타입에 따라 적절한 컬럼에 값 저장
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemProperty {

    /**
     * 아이템 속성값 ID (PK)
     */
    private Long itemPropertyId;

    /**
     * 아이템 ID (FK)
     */
    private Long itemId;

    /**
     * 속성 정의 ID (FK)
     */
    private Long propertyId;

    /**
     * 텍스트 값 (TEXT, SELECT, MULTI_SELECT, CHECKBOX)
     */
    private String valueText;

    /**
     * 숫자 값 (NUMBER)
     */
    private BigDecimal valueNumber;

    /**
     * 날짜 값 (DATE)
     */
    private LocalDate valueDate;

    /**
     * 사용자 ID 값 (USER)
     */
    private Long valueUserId;

    /**
     * 선택 옵션 ID 값 (SELECT)
     */
    private Long valueOptionId;

    /**
     * 체크박스 값 (CHECKBOX: Y/N)
     */
    private String valueCheckbox;

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
    // 추가 필드 (Mapper에서 JOIN으로 설정)
    // =============================================

    /**
     * 속성명
     */
    private String propertyName;

    /**
     * 속성 타입
     */
    private String propertyType;

    /**
     * 사용자 값일 때 사용자명
     */
    private String valueUserName;

    /**
     * SELECT 값일 때 옵션명
     */
    private String optionName;

    /**
     * SELECT 값일 때 옵션 색상
     */
    private String optionColor;

    // =============================================
    // 편의 메서드
    // =============================================

    /**
     * 속성 타입에 따른 값 반환
     */
    public Object getValue() {
        if (propertyType == null) {
            return valueText;
        }

        return switch (propertyType) {
            case PropertyDef.TYPE_TEXT -> valueText;
            case PropertyDef.TYPE_CHECKBOX -> valueCheckbox != null ? valueCheckbox : valueText;
            case PropertyDef.TYPE_NUMBER -> valueNumber;
            case PropertyDef.TYPE_DATE -> valueDate;
            case PropertyDef.TYPE_USER -> valueUserId;
            case PropertyDef.TYPE_SELECT -> valueOptionId != null ? valueOptionId : valueText;
            case PropertyDef.TYPE_MULTI_SELECT -> valueText; // 다중선택은 별도 테이블 사용
            default -> valueText;
        };
    }

    /**
     * 값 설정 (타입에 따라 적절한 필드에 설정)
     */
    public void setValue(String type, Object value) {
        this.propertyType = type;

        if (value == null) {
            clearValues();
            return;
        }

        switch (type) {
            case PropertyDef.TYPE_TEXT, PropertyDef.TYPE_MULTI_SELECT -> {
                this.valueText = value.toString();
            }
            case PropertyDef.TYPE_CHECKBOX -> {
                this.valueCheckbox = value.toString();
            }
            case PropertyDef.TYPE_SELECT -> {
                if (value instanceof Long) {
                    this.valueOptionId = (Long) value;
                } else {
                    try {
                        this.valueOptionId = Long.parseLong(value.toString());
                    } catch (NumberFormatException e) {
                        // 옵션 ID가 아닌 텍스트 값인 경우
                        this.valueText = value.toString();
                    }
                }
            }
            case PropertyDef.TYPE_NUMBER -> {
                if (value instanceof BigDecimal) {
                    this.valueNumber = (BigDecimal) value;
                } else if (value instanceof Number) {
                    this.valueNumber = new BigDecimal(value.toString());
                } else {
                    this.valueNumber = new BigDecimal(value.toString());
                }
            }
            case PropertyDef.TYPE_DATE -> {
                if (value instanceof LocalDate) {
                    this.valueDate = (LocalDate) value;
                } else {
                    this.valueDate = LocalDate.parse(value.toString());
                }
            }
            case PropertyDef.TYPE_USER -> {
                if (value instanceof Long) {
                    this.valueUserId = (Long) value;
                } else {
                    this.valueUserId = Long.parseLong(value.toString());
                }
            }
        }
    }

    /**
     * 모든 값 필드 초기화
     */
    private void clearValues() {
        this.valueText = null;
        this.valueNumber = null;
        this.valueDate = null;
        this.valueUserId = null;
        this.valueOptionId = null;
        this.valueCheckbox = null;
    }
}
