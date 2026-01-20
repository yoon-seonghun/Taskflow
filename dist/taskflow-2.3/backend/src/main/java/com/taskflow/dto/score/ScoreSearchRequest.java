package com.taskflow.dto.score;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 성과점수 검색 요청 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreSearchRequest {

    /**
     * 담당자 USERNAME
     */
    private String assigneeUsername;

    /**
     * 부서 코드
     */
    private String deptCode;

    /**
     * 보드 ID
     */
    private Long boardId;

    /**
     * 시작일
     */
    private LocalDate startDate;

    /**
     * 종료일
     */
    private LocalDate endDate;

    /**
     * 승인 상태 (PENDING, APPROVED, REJECTED)
     */
    private String approvalStatus;

    /**
     * 등급 필터 (PERFECT, EXCELLENT, GOOD, FAIR, POOR)
     */
    private String baseGrade;

    /**
     * 정렬 필드
     */
    @Builder.Default
    private String sortBy = "completedAt";

    /**
     * 정렬 방향
     */
    @Builder.Default
    private String sortDirection = "DESC";

    /**
     * 페이지 번호 (0부터 시작)
     */
    @Builder.Default
    private Integer page = 0;

    /**
     * 페이지 크기
     */
    @Builder.Default
    private Integer size = 20;

    /**
     * offset 계산 (MyBatis OFFSET 절에서 사용)
     */
    public Integer getOffset() {
        return page * size;
    }
}
