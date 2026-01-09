package com.taskflow.dto.score;

import com.taskflow.domain.ItemScore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 성과점수 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreResponse {

    private Long scoreId;
    private Long itemId;
    private String itemTitle;
    private String assigneeUsername;
    private String assigneeName;

    // 점수 정보
    private String baseGrade;
    private BigDecimal baseScore;
    private BigDecimal weightFactor;
    private BigDecimal finalScore;

    // 가중치 요소
    private Integer difficulty;
    private Integer scopeChange;
    private Integer riskHandling;

    // 승인 정보
    private String approvalStatus;
    private String approvedBy;
    private LocalDateTime approvedAt;

    // 날짜 정보
    private LocalDate requestDate;
    private LocalDate dueDate;
    private LocalDate completedAt;
    private LocalDateTime createdAt;
    private String createdBy;

    public static ScoreResponse from(ItemScore score) {
        if (score == null) return null;

        return ScoreResponse.builder()
                .scoreId(score.getScoreId())
                .itemId(score.getItemId())
                .itemTitle(score.getItemTitle())
                .assigneeUsername(score.getAssigneeUsername())
                .assigneeName(score.getAssigneeName())
                .baseGrade(score.getBaseGrade())
                .baseScore(score.getBaseScore())
                .weightFactor(score.getWeightFactor())
                .finalScore(score.getFinalScore())
                .difficulty(score.getDifficulty())
                .scopeChange(score.getScopeChange())
                .riskHandling(score.getRiskHandling())
                .approvalStatus(score.getApprovalStatus())
                .approvedBy(score.getApprovedBy())
                .approvedAt(score.getApprovedAt())
                .requestDate(score.getRequestDate())
                .dueDate(score.getDueDate())
                .completedAt(score.getCompletedAt())
                .createdAt(score.getCreatedAt())
                .createdBy(score.getCreatedBy())
                .build();
    }
}
