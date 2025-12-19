package com.taskflow.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 속성 옵션 엔티티
 *
 * 테이블: TB_PROPERTY_OPTION
 *
 * SELECT, MULTI_SELECT 타입 속성의 선택 옵션
 * - 속성별로 여러 옵션을 가질 수 있음
 * - 칸반 뷰, 태그 표시용 색상 지원
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PropertyOption {

    /**
     * 옵션 ID (PK)
     */
    private Long optionId;

    /**
     * 속성 정의 ID (FK)
     */
    private Long propertyId;

    /**
     * 옵션명 (표시값) - DB의 OPTION_LABEL과 매핑
     */
    private String optionName;

    /**
     * 표시 색상 (#RRGGBB)
     */
    private String color;

    /**
     * 표시 순서
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
     * 보드 ID
     */
    private Long boardId;

    /**
     * 사용 중인 아이템 수
     */
    private Integer usageCount;

    // =============================================
    // 편의 메서드
    // =============================================

    /**
     * 활성 옵션 여부
     */
    public boolean isActive() {
        return "Y".equals(useYn);
    }

    /**
     * 사용 중인지 확인
     */
    public boolean isInUse() {
        return usageCount != null && usageCount > 0;
    }
}
