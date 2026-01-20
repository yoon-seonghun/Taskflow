package com.taskflow.dto.score;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 성과점수 계산 요청 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreCalculateRequest {

    /**
     * 업무 ID
     */
    @NotNull(message = "업무 ID는 필수입니다")
    private Long itemId;

    /**
     * 완료일 (null이면 오늘)
     */
    private LocalDate completedAt;

    /**
     * 난이도 (1-5)
     */
    private Integer difficulty;

    /**
     * 범위변경 (0: 없음, 1: 축소, 2: 확대, 3: 극단적, 4: 카오스)
     */
    private Integer scopeChange;

    /**
     * 리스크대응 (0: 해당없음, 1: 정상, 2: 우수, 3: 탁월)
     */
    private Integer riskHandling;

    /**
     * PM 승인 여부 (난이도/범위변경이 극단적 이상일 때 필요)
     */
    private Boolean pmApproved;

    /**
     * PM 승인자 USERNAME
     */
    private String approvedBy;
}
