package com.taskflow.mapper;

import com.taskflow.domain.ItemScore;
import com.taskflow.dto.score.ScoreSearchRequest;
import com.taskflow.dto.score.ScoreSummaryResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 성과점수 Mapper 인터페이스
 */
@Mapper
public interface ItemScoreMapper {

    // =============================================
    // 조회
    // =============================================

    /**
     * ID로 점수 조회
     */
    Optional<ItemScore> findById(@Param("scoreId") Long scoreId);

    /**
     * 업무 ID로 점수 조회
     */
    Optional<ItemScore> findByItemId(@Param("itemId") Long itemId);

    /**
     * 담당자별 점수 목록 조회
     */
    List<ItemScore> findByAssignee(@Param("assigneeUsername") String assigneeUsername,
                                    @Param("startDate") LocalDate startDate,
                                    @Param("endDate") LocalDate endDate);

    /**
     * 검색 조건으로 점수 목록 조회
     */
    List<ItemScore> search(ScoreSearchRequest request);

    /**
     * 검색 결과 총 개수
     */
    int countBySearch(ScoreSearchRequest request);

    /**
     * 담당자별 점수 요약 조회
     */
    ScoreSummaryResponse getSummaryByAssignee(@Param("assigneeUsername") String assigneeUsername,
                                               @Param("startDate") LocalDate startDate,
                                               @Param("endDate") LocalDate endDate);

    /**
     * 부서별 점수 요약 목록 조회
     */
    List<ScoreSummaryResponse> getSummaryByDepartment(@Param("deptCode") String deptCode,
                                                       @Param("startDate") LocalDate startDate,
                                                       @Param("endDate") LocalDate endDate);

    /**
     * 승인 대기 점수 목록 조회
     */
    List<ItemScore> findPendingApproval(@Param("deptCode") String deptCode);

    // =============================================
    // 등록/수정/삭제
    // =============================================

    /**
     * 점수 등록
     */
    int insert(ItemScore score);

    /**
     * 점수 수정
     */
    int update(ItemScore score);

    /**
     * 승인 상태 업데이트
     */
    int updateApprovalStatus(@Param("scoreId") Long scoreId,
                             @Param("status") String status,
                             @Param("approvedBy") String approvedBy);

    /**
     * 가중치 요소 업데이트
     */
    int updateWeightFactors(@Param("scoreId") Long scoreId,
                            @Param("difficulty") Integer difficulty,
                            @Param("scopeChange") Integer scopeChange,
                            @Param("riskHandling") Integer riskHandling,
                            @Param("weightFactor") java.math.BigDecimal weightFactor,
                            @Param("finalScore") java.math.BigDecimal finalScore);

    /**
     * 점수 삭제
     */
    int delete(@Param("scoreId") Long scoreId);

    /**
     * 업무별 점수 삭제
     */
    int deleteByItemId(@Param("itemId") Long itemId);
}
