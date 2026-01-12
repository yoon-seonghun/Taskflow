package com.taskflow.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 업무 성과 점수 엔티티
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemScore {

    /**
     * 점수 ID
     */
    private Long scoreId;

    /**
     * 업무 ID
     */
    private Long itemId;

    /**
     * 담당자 USERNAME
     */
    private String assigneeUsername;

    // ============================================
    // 기본 점수
    // ============================================

    /**
     * 기본 등급 (PERFECT, EXCELLENT, GOOD, FAIR, POOR)
     * - PERFECT: 50% 이내 완료
     * - EXCELLENT: 75% 이내 완료
     * - GOOD: 기한 내 완료
     * - FAIR: 20% 초과
     * - POOR: 20% 이상 초과
     */
    private String baseGrade;

    /**
     * 기본 점수
     * - PERFECT: 100
     * - EXCELLENT: 90
     * - GOOD: 80
     * - FAIR: 60
     * - POOR: 40
     */
    private BigDecimal baseScore;

    // ============================================
    // 가중치 요소
    // ============================================

    /**
     * 난이도 (1~5)
     * - 1: 기본
     * - 5: 최고 난이도
     */
    private Integer difficulty;

    /**
     * 범위변경 (0~2)
     * - 0: 없음
     * - 1: 소규모
     * - 2: 대규모
     */
    private Integer scopeChange;

    /**
     * 리스크대응 (0~2)
     * - 0: 없음
     * - 1: 일반
     * - 2: 긴급
     */
    private Integer riskHandling;

    /**
     * 가중치 (계산값)
     * 기본 1.0 + 난이도 보정 + 범위변경 보정 + 리스크대응 보정
     */
    private BigDecimal weightFactor;

    // ============================================
    // 최종 점수
    // ============================================

    /**
     * 최종 점수 = 기본점수 × 가중치
     */
    private BigDecimal finalScore;

    // ============================================
    // 승인 정보
    // ============================================

    /**
     * 승인 상태 (PENDING, APPROVED, REJECTED)
     */
    private String approvalStatus;

    /**
     * 승인자 USERNAME
     */
    private String approvedBy;

    /**
     * 승인일시
     */
    private LocalDateTime approvedAt;

    // ============================================
    // 날짜 정보 (스냅샷)
    // ============================================

    /**
     * 요청일
     */
    private LocalDate requestDate;

    /**
     * 마감일
     */
    private LocalDate dueDate;

    /**
     * 완료일
     */
    private LocalDate completedAt;

    // ============================================
    // 감사 정보
    // ============================================

    /**
     * 생성일시
     */
    private LocalDateTime createdAt;

    /**
     * 생성자 USERNAME
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
    // 조회용 필드 (JOIN)
    // ============================================

    /**
     * 업무 제목
     */
    private String itemTitle;

    /**
     * 담당자 이름
     */
    private String assigneeName;

    // ============================================
    // 등급 상수
    // ============================================

    public static final String GRADE_PERFECT = "PERFECT";
    public static final String GRADE_EXCELLENT = "EXCELLENT";
    public static final String GRADE_GOOD = "GOOD";
    public static final String GRADE_FAIR = "FAIR";
    public static final String GRADE_POOR = "POOR";

    // ============================================
    // 승인 상태 상수
    // ============================================

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";
}
