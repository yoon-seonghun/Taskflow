package com.taskflow.dto.score;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 성과점수 요약 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreSummaryResponse {

    /**
     * 담당자 USERNAME
     */
    private String assigneeUsername;

    /**
     * 담당자 이름
     */
    private String assigneeName;

    /**
     * 완료 건수
     */
    private Integer completedCount;

    /**
     * 총 점수
     */
    private BigDecimal totalScore;

    /**
     * 평균 점수
     */
    private BigDecimal averageScore;

    /**
     * PERFECT 등급 건수
     */
    private Integer perfectCount;

    /**
     * EXCELLENT 등급 건수
     */
    private Integer excellentCount;

    /**
     * GOOD 등급 건수
     */
    private Integer goodCount;

    /**
     * FAIR 등급 건수
     */
    private Integer fairCount;

    /**
     * POOR 등급 건수
     */
    private Integer poorCount;

    /**
     * 평균 난이도
     */
    private BigDecimal averageDifficulty;

    /**
     * 평균 가중치
     */
    private BigDecimal averageWeightFactor;
}
