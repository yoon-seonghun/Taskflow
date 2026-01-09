package com.taskflow.service;

import com.taskflow.domain.ItemScore;
import com.taskflow.dto.score.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 성과점수 서비스 인터페이스
 */
public interface ItemScoreService {

    // =============================================
    // 조회
    // =============================================

    /**
     * ID로 점수 조회
     */
    Optional<ItemScore> findById(Long scoreId);

    /**
     * 업무 ID로 점수 조회
     */
    Optional<ItemScore> findByItemId(Long itemId);

    /**
     * 담당자별 점수 목록 조회
     */
    List<ItemScore> findByAssignee(String assigneeUsername, LocalDate startDate, LocalDate endDate);

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
    ScoreSummaryResponse getSummaryByAssignee(String assigneeUsername, LocalDate startDate, LocalDate endDate);

    /**
     * 부서별 점수 요약 목록 조회
     */
    List<ScoreSummaryResponse> getSummaryByDepartment(String deptCode, LocalDate startDate, LocalDate endDate);

    /**
     * 승인 대기 점수 목록 조회
     */
    List<ItemScore> findPendingApproval(String deptCode);

    // =============================================
    // 점수 계산
    // =============================================

    /**
     * 업무 완료 시 점수 계산 및 등록
     * @param request 점수 계산 요청
     * @param createdBy 생성자 username
     * @return 생성된 점수 정보
     */
    ScoreResponse calculateAndSave(ScoreCalculateRequest request, String createdBy);

    /**
     * 기본 등급 계산 (소요시간 기준)
     * @param requestDate 요청일
     * @param dueDate 마감일
     * @param completedAt 완료일
     * @return 등급 (PERFECT, EXCELLENT, GOOD, FAIR, POOR)
     */
    String calculateBaseGrade(LocalDate requestDate, LocalDate dueDate, LocalDate completedAt);

    /**
     * 기본 점수 계산 (등급 기준)
     * @param baseGrade 등급
     * @return 기본 점수
     */
    java.math.BigDecimal calculateBaseScore(String baseGrade);

    /**
     * 가중치 계산
     * @param difficulty 난이도 (1-5)
     * @param scopeChange 범위변경 (0-2)
     * @param riskHandling 리스크대응 (0-2)
     * @return 가중치
     */
    java.math.BigDecimal calculateWeightFactor(Integer difficulty, Integer scopeChange, Integer riskHandling);

    // =============================================
    // 수정
    // =============================================

    /**
     * 점수 정보 수정
     */
    void update(ItemScore score);

    /**
     * 가중치 요소 수정 (PM 조정)
     */
    void updateWeightFactors(Long scoreId, Integer difficulty, Integer scopeChange,
                             Integer riskHandling, String updatedBy);

    // =============================================
    // 승인 처리
    // =============================================

    /**
     * 점수 승인/거부 처리
     * @param scoreId 점수 ID
     * @param request 승인 요청
     * @param approvedBy 승인자 username
     */
    void processApproval(Long scoreId, ScoreApprovalRequest request, String approvedBy);

    // =============================================
    // 삭제
    // =============================================

    /**
     * 점수 삭제
     */
    void delete(Long scoreId);

    /**
     * 업무별 점수 삭제
     */
    void deleteByItemId(Long itemId);

    // =============================================
    // 권한 확인
    // =============================================

    /**
     * PM 승인 필요 여부 확인
     * @param difficulty 난이도
     * @param scopeChange 범위변경
     * @param riskHandling 리스크대응
     * @return 승인 필요 여부
     */
    boolean requiresApproval(Integer difficulty, Integer scopeChange, Integer riskHandling);

    /**
     * 점수 조회 권한 확인
     * @param scoreId 점수 ID
     * @param username 사용자 username
     * @param userRole 사용자 역할
     * @param userDeptCode 사용자 부서코드
     * @return 조회 권한 여부
     */
    boolean hasViewPermission(Long scoreId, String username, String userRole, String userDeptCode);

    /**
     * 점수 수정 권한 확인 (PM만 가능)
     */
    boolean hasEditPermission(String username, String userRole);
}
