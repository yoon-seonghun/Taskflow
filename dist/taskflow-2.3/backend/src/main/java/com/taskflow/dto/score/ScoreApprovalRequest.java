package com.taskflow.dto.score;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 성과점수 승인 요청 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreApprovalRequest {

    /**
     * 승인 상태 (APPROVED, REJECTED)
     */
    @NotBlank(message = "승인 상태는 필수입니다")
    @Pattern(regexp = "^(APPROVED|REJECTED)$", message = "승인 상태는 APPROVED 또는 REJECTED만 가능합니다")
    private String status;

    /**
     * 거부 사유 (거부 시)
     */
    private String rejectReason;

    /**
     * 수정된 난이도 (승인 시 변경 가능)
     */
    private Integer difficulty;

    /**
     * 수정된 범위변경 (승인 시 변경 가능)
     */
    private Integer scopeChange;

    /**
     * 수정된 리스크대응 (승인 시 변경 가능)
     */
    private Integer riskHandling;
}
