package com.taskflow.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 업무 속성 변경 이력 엔티티
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemPropertyHistory {

    /**
     * 이력 ID
     */
    private Long historyId;

    /**
     * 업무 ID
     */
    private Long itemId;

    /**
     * 속성 ID (NULL이면 기본 속성)
     */
    private Long propertyId;

    /**
     * 속성명 (스냅샷)
     */
    private String propertyName;

    /**
     * 속성 타입
     */
    private String propertyType;

    /**
     * 이벤트 유형 (CREATE, INITIAL, UPDATE, COMPLETE)
     */
    private String eventType;

    /**
     * 이전 텍스트값
     */
    private String oldValueText;

    /**
     * 새 텍스트값
     */
    private String newValueText;

    /**
     * 이전 날짜값
     */
    private LocalDate oldValueDate;

    /**
     * 새 날짜값
     */
    private LocalDate newValueDate;

    /**
     * 이전 숫자값
     */
    private BigDecimal oldValueNumber;

    /**
     * 새 숫자값
     */
    private BigDecimal newValueNumber;

    /**
     * 변경 사유
     */
    private String changeReason;

    /**
     * 변경일시
     */
    private LocalDateTime changedAt;

    /**
     * 변경자 USERNAME
     */
    private String changedBy;

    // ============================================
    // 조회용 필드
    // ============================================

    /**
     * 변경자 이름
     */
    private String changedByName;

    /**
     * 업무 제목 (조회용)
     */
    private String itemContent;

    // ============================================
    // 이벤트 유형 상수
    // ============================================

    public static final String EVENT_CREATE = "CREATE";
    public static final String EVENT_INITIAL = "INITIAL";
    public static final String EVENT_UPDATE = "UPDATE";
    public static final String EVENT_COMPLETE = "COMPLETE";
}
