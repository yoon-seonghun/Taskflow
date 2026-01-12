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
 * USERNAME 기반 FK 참조 시스템:
 * - VALUE_USERNAME: 사용자 타입 속성값 (USERNAME 참조)
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
     * 사용자 USERNAME 값 (USER 타입)
     */
    private String valueUsername;

    /**
     * 선택 옵션 ID 값 (SELECT)
     */
    private Long valueOptionId;

    /**
     * 체크박스 값 (CHECKBOX: Y/N)
     */
    private String valueCheckbox;

    /**
     * 정렬 순서
     */
    private Integer sortOrder;

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
     * 속성 소유자 타입 (GLOBAL, MANAGER, USER)
     */
    private String ownerType;

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
            case PropertyDef.TYPE_USER -> valueUsername;
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
                // 빈 문자열은 null로 처리 (선택됨 상태 유지)
                String textValue = value.toString();
                this.valueText = (textValue == null || textValue.trim().isEmpty()) ? null : textValue;
            }
            case PropertyDef.TYPE_CHECKBOX -> {
                // 체크박스 값을 'Y' 또는 'N'으로 정규화 (DB 컬럼이 CHAR(1))
                String checkValue = value.toString();
                if (checkValue == null || checkValue.trim().isEmpty()) {
                    this.valueCheckbox = null;
                } else {
                    // 'true', '1', 'Y', 'yes' 등은 'Y'로, 나머지는 'N'으로 변환
                    String normalized = checkValue.trim().toLowerCase();
                    this.valueCheckbox = ("true".equals(normalized) || "1".equals(normalized)
                            || "y".equals(normalized) || "yes".equals(normalized)) ? "Y" : "N";
                }
            }
            case PropertyDef.TYPE_SELECT -> {
                // 빈 문자열은 null로 처리
                String selectStr = value.toString();
                if (selectStr == null || selectStr.trim().isEmpty()) {
                    this.valueOptionId = null;
                    this.valueText = null;
                } else if (value instanceof Long) {
                    this.valueOptionId = (Long) value;
                } else {
                    try {
                        this.valueOptionId = Long.parseLong(selectStr);
                    } catch (NumberFormatException e) {
                        // 옵션 ID가 아닌 텍스트 값인 경우
                        this.valueText = selectStr;
                    }
                }
            }
            case PropertyDef.TYPE_NUMBER -> {
                // 빈 문자열은 null로 처리
                String numStr = value.toString();
                if (numStr == null || numStr.trim().isEmpty()) {
                    this.valueNumber = null;
                } else if (value instanceof BigDecimal) {
                    this.valueNumber = (BigDecimal) value;
                } else if (value instanceof Number) {
                    this.valueNumber = new BigDecimal(numStr);
                } else {
                    this.valueNumber = new BigDecimal(numStr);
                }
            }
            case PropertyDef.TYPE_DATE -> {
                // 빈 문자열은 null로 처리
                String dateStr = value.toString();
                if (dateStr == null || dateStr.trim().isEmpty()) {
                    this.valueDate = null;
                } else if (value instanceof LocalDate) {
                    this.valueDate = (LocalDate) value;
                } else {
                    this.valueDate = LocalDate.parse(dateStr);
                }
            }
            case PropertyDef.TYPE_USER -> {
                // USERNAME (String) - 빈 문자열은 null로 처리 (FK 제약 조건)
                String username = value.toString();
                this.valueUsername = (username == null || username.trim().isEmpty()) ? null : username;
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
        this.valueUsername = null;
        this.valueOptionId = null;
        this.valueCheckbox = null;
    }
}
