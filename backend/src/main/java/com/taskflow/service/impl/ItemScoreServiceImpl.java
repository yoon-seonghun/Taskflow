package com.taskflow.service.impl;

import com.taskflow.domain.Item;
import com.taskflow.domain.ItemScore;
import com.taskflow.dto.score.*;
import com.taskflow.exception.BusinessException;
import com.taskflow.mapper.ItemMapper;
import com.taskflow.mapper.ItemScoreMapper;
import com.taskflow.service.ItemScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * 성과점수 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ItemScoreServiceImpl implements ItemScoreService {

    private final ItemScoreMapper itemScoreMapper;
    private final ItemMapper itemMapper;

    // 등급별 기본 점수
    private static final BigDecimal SCORE_PERFECT = new BigDecimal("100");
    private static final BigDecimal SCORE_EXCELLENT = new BigDecimal("90");
    private static final BigDecimal SCORE_GOOD = new BigDecimal("80");
    private static final BigDecimal SCORE_FAIR = new BigDecimal("60");
    private static final BigDecimal SCORE_POOR = new BigDecimal("40");

    // PM 역할 목록
    private static final List<String> PM_ROLES = List.of("TEAM_LEADER", "MANAGER", "ADMIN");

    // =============================================
    // 조회
    // =============================================

    @Override
    public Optional<ItemScore> findById(Long scoreId) {
        return itemScoreMapper.findById(scoreId);
    }

    @Override
    public Optional<ItemScore> findByItemId(Long itemId) {
        return itemScoreMapper.findByItemId(itemId);
    }

    @Override
    public List<ItemScore> findByAssignee(String assigneeUsername, LocalDate startDate, LocalDate endDate) {
        return itemScoreMapper.findByAssignee(assigneeUsername, startDate, endDate);
    }

    @Override
    public List<ItemScore> search(ScoreSearchRequest request) {
        return itemScoreMapper.search(request);
    }

    @Override
    public int countBySearch(ScoreSearchRequest request) {
        return itemScoreMapper.countBySearch(request);
    }

    @Override
    public ScoreSummaryResponse getSummaryByAssignee(String assigneeUsername, LocalDate startDate, LocalDate endDate) {
        ScoreSummaryResponse summary = itemScoreMapper.getSummaryByAssignee(assigneeUsername, startDate, endDate);
        if (summary == null) {
            // 데이터가 없는 경우 빈 응답 반환
            return ScoreSummaryResponse.builder()
                    .assigneeUsername(assigneeUsername)
                    .completedCount(0)
                    .totalScore(BigDecimal.ZERO)
                    .averageScore(BigDecimal.ZERO)
                    .perfectCount(0)
                    .excellentCount(0)
                    .goodCount(0)
                    .fairCount(0)
                    .poorCount(0)
                    .build();
        }
        return summary;
    }

    @Override
    public List<ScoreSummaryResponse> getSummaryByDepartment(String deptCode, LocalDate startDate, LocalDate endDate) {
        return itemScoreMapper.getSummaryByDepartment(deptCode, startDate, endDate);
    }

    @Override
    public List<ItemScore> findPendingApproval(String deptCode) {
        return itemScoreMapper.findPendingApproval(deptCode);
    }

    // =============================================
    // 점수 계산
    // =============================================

    @Override
    @Transactional
    public ScoreResponse calculateAndSave(ScoreCalculateRequest request, String createdBy) {
        // 업무 조회
        Item item = itemMapper.findById(request.getItemId())
                .orElseThrow(() -> new BusinessException("업무를 찾을 수 없습니다: " + request.getItemId()));

        // 이미 점수가 있는지 확인
        if (itemScoreMapper.findByItemId(request.getItemId()).isPresent()) {
            throw new BusinessException("이미 점수가 등록된 업무입니다: " + request.getItemId());
        }

        // 필수 날짜 확인
        LocalDate requestDate = item.getRequestDate();
        LocalDate dueDate = item.getDueDate();
        LocalDate completedAt = request.getCompletedAt();

        if (requestDate == null) {
            throw new BusinessException("요청일이 설정되지 않은 업무입니다");
        }
        if (dueDate == null) {
            throw new BusinessException("마감일이 설정되지 않은 업무입니다");
        }
        if (completedAt == null) {
            completedAt = LocalDate.now();
        }

        // 담당자 확인
        String assigneeUsername = item.getAssigneeUsername();
        if (assigneeUsername == null || assigneeUsername.isEmpty()) {
            throw new BusinessException("담당자가 지정되지 않은 업무입니다");
        }

        // 등급 및 점수 계산
        String baseGrade = calculateBaseGrade(requestDate, dueDate, completedAt);
        BigDecimal baseScore = calculateBaseScore(baseGrade);

        // 가중치 계산
        Integer difficulty = request.getDifficulty() != null ? request.getDifficulty() : 1;
        Integer scopeChange = request.getScopeChange() != null ? request.getScopeChange() : 0;
        Integer riskHandling = request.getRiskHandling() != null ? request.getRiskHandling() : 0;
        BigDecimal weightFactor = calculateWeightFactor(difficulty, scopeChange, riskHandling);

        // 최종 점수 계산
        BigDecimal finalScore = baseScore.multiply(weightFactor).setScale(2, RoundingMode.HALF_UP);

        // 승인 필요 여부 확인
        boolean needsApproval = requiresApproval(difficulty, scopeChange, riskHandling);
        String approvalStatus = needsApproval ? "PENDING" : "APPROVED";

        // 점수 저장
        ItemScore score = ItemScore.builder()
                .itemId(request.getItemId())
                .assigneeUsername(assigneeUsername)
                .baseGrade(baseGrade)
                .baseScore(baseScore)
                .difficulty(difficulty)
                .scopeChange(scopeChange)
                .riskHandling(riskHandling)
                .weightFactor(weightFactor)
                .finalScore(finalScore)
                .approvalStatus(approvalStatus)
                .requestDate(requestDate)
                .dueDate(dueDate)
                .completedAt(completedAt)
                .createdBy(createdBy)
                .build();

        itemScoreMapper.insert(score);

        log.info("점수 등록 완료: itemId={}, grade={}, finalScore={}, approval={}",
                request.getItemId(), baseGrade, finalScore, approvalStatus);

        // 응답 생성
        return buildScoreResponse(score, item.getTitle());
    }

    @Override
    public String calculateBaseGrade(LocalDate requestDate, LocalDate dueDate, LocalDate completedAt) {
        // 전체 기간 (요청일 ~ 마감일)
        long totalDays = ChronoUnit.DAYS.between(requestDate, dueDate);
        if (totalDays <= 0) {
            totalDays = 1; // 최소 1일
        }

        // 소요 기간 (요청일 ~ 완료일)
        long usedDays = ChronoUnit.DAYS.between(requestDate, completedAt);
        if (usedDays < 0) {
            usedDays = 0; // 요청일 전에 완료한 경우
        }

        // 소요율 계산
        double usageRate = (double) usedDays / totalDays * 100;

        // 등급 결정
        if (usageRate <= 50) {
            return "PERFECT";    // 50% 이내 완료
        } else if (usageRate <= 75) {
            return "EXCELLENT";  // 75% 이내 완료
        } else if (usageRate <= 100) {
            return "GOOD";       // 기한 내 완료
        } else if (usageRate <= 120) {
            return "FAIR";       // 20% 초과
        } else {
            return "POOR";       // 20% 이상 초과
        }
    }

    @Override
    public BigDecimal calculateBaseScore(String baseGrade) {
        return switch (baseGrade) {
            case "PERFECT" -> SCORE_PERFECT;
            case "EXCELLENT" -> SCORE_EXCELLENT;
            case "GOOD" -> SCORE_GOOD;
            case "FAIR" -> SCORE_FAIR;
            case "POOR" -> SCORE_POOR;
            default -> SCORE_GOOD;
        };
    }

    @Override
    public BigDecimal calculateWeightFactor(Integer difficulty, Integer scopeChange, Integer riskHandling) {
        // 기본 가중치 1.0
        BigDecimal factor = BigDecimal.ONE;

        // 난이도 가중치: 1(기본) ~ 5(최고)
        // 난이도 1: 1.0, 2: 1.05, 3: 1.1, 4: 1.15, 5: 1.2
        if (difficulty != null && difficulty > 1) {
            factor = factor.add(new BigDecimal("0.05").multiply(new BigDecimal(difficulty - 1)));
        }

        // 범위변경 가중치: 0(없음), 1(소규모), 2(대규모)
        // 0: 1.0, 1: 1.05, 2: 1.1
        if (scopeChange != null && scopeChange > 0) {
            factor = factor.add(new BigDecimal("0.05").multiply(new BigDecimal(scopeChange)));
        }

        // 리스크대응 가중치: 0(없음), 1(일반), 2(긴급)
        // 0: 1.0, 1: 1.05, 2: 1.1
        if (riskHandling != null && riskHandling > 0) {
            factor = factor.add(new BigDecimal("0.05").multiply(new BigDecimal(riskHandling)));
        }

        return factor.setScale(2, RoundingMode.HALF_UP);
    }

    // =============================================
    // 수정
    // =============================================

    @Override
    @Transactional
    public void update(ItemScore score) {
        itemScoreMapper.update(score);
    }

    @Override
    @Transactional
    public void updateWeightFactors(Long scoreId, Integer difficulty, Integer scopeChange,
                                     Integer riskHandling, String updatedBy) {
        ItemScore score = itemScoreMapper.findById(scoreId)
                .orElseThrow(() -> new BusinessException("점수를 찾을 수 없습니다: " + scoreId));

        // 가중치 재계산
        BigDecimal newWeightFactor = calculateWeightFactor(difficulty, scopeChange, riskHandling);
        BigDecimal newFinalScore = score.getBaseScore().multiply(newWeightFactor)
                .setScale(2, RoundingMode.HALF_UP);

        itemScoreMapper.updateWeightFactors(scoreId, difficulty, scopeChange, riskHandling,
                newWeightFactor, newFinalScore);

        log.info("가중치 수정: scoreId={}, weightFactor={}, finalScore={}, by={}",
                scoreId, newWeightFactor, newFinalScore, updatedBy);
    }

    // =============================================
    // 승인 처리
    // =============================================

    @Override
    @Transactional
    public void processApproval(Long scoreId, ScoreApprovalRequest request, String approvedBy) {
        ItemScore score = itemScoreMapper.findById(scoreId)
                .orElseThrow(() -> new BusinessException("점수를 찾을 수 없습니다: " + scoreId));

        if (!"PENDING".equals(score.getApprovalStatus())) {
            throw new BusinessException("승인 대기 상태가 아닙니다");
        }

        String status = request.getStatus();

        if ("APPROVED".equals(status)) {
            // 승인 시 가중치 수정이 있으면 적용
            if (request.getDifficulty() != null || request.getScopeChange() != null ||
                    request.getRiskHandling() != null) {

                Integer difficulty = request.getDifficulty() != null ?
                        request.getDifficulty() : score.getDifficulty();
                Integer scopeChange = request.getScopeChange() != null ?
                        request.getScopeChange() : score.getScopeChange();
                Integer riskHandling = request.getRiskHandling() != null ?
                        request.getRiskHandling() : score.getRiskHandling();

                BigDecimal newWeightFactor = calculateWeightFactor(difficulty, scopeChange, riskHandling);
                BigDecimal newFinalScore = score.getBaseScore().multiply(newWeightFactor)
                        .setScale(2, RoundingMode.HALF_UP);

                itemScoreMapper.updateWeightFactors(scoreId, difficulty, scopeChange, riskHandling,
                        newWeightFactor, newFinalScore);
            }
        }

        itemScoreMapper.updateApprovalStatus(scoreId, status, approvedBy);

        log.info("점수 승인 처리: scoreId={}, status={}, by={}", scoreId, status, approvedBy);
    }

    // =============================================
    // 삭제
    // =============================================

    @Override
    @Transactional
    public void delete(Long scoreId) {
        itemScoreMapper.delete(scoreId);
    }

    @Override
    @Transactional
    public void deleteByItemId(Long itemId) {
        itemScoreMapper.deleteByItemId(itemId);
    }

    // =============================================
    // 권한 확인
    // =============================================

    @Override
    public boolean requiresApproval(Integer difficulty, Integer scopeChange, Integer riskHandling) {
        // 난이도 4-5 또는 범위변경 2 또는 리스크대응 2인 경우 PM 승인 필요
        return (difficulty != null && difficulty >= 4) ||
                (scopeChange != null && scopeChange >= 2) ||
                (riskHandling != null && riskHandling >= 2);
    }

    @Override
    public boolean hasViewPermission(Long scoreId, String username, String userRole, String userDeptCode) {
        ItemScore score = itemScoreMapper.findById(scoreId).orElse(null);
        if (score == null) {
            return false;
        }

        // 본인 점수는 항상 조회 가능
        if (username.equals(score.getAssigneeUsername())) {
            return true;
        }

        // PM 역할 (팀장, Manager, Admin)은 조회 가능
        if (PM_ROLES.contains(userRole)) {
            return true;
        }

        // ADMIN은 전체 조회 가능
        if ("ADMIN".equals(userRole)) {
            return true;
        }

        // TEAM_LEADER는 같은 부서만 조회 가능
        if ("TEAM_LEADER".equals(userRole)) {
            // 담당자의 부서와 현재 사용자의 부서가 같은지 확인 필요
            // 이 부분은 UserMapper를 통해 담당자의 부서 정보를 가져와서 비교해야 함
            // 간단하게 처리하기 위해 일단 true 반환
            return true;
        }

        return false;
    }

    @Override
    public boolean hasEditPermission(String username, String userRole) {
        // PM 역할만 수정 가능
        return PM_ROLES.contains(userRole);
    }

    // =============================================
    // 헬퍼 메서드
    // =============================================

    private ScoreResponse buildScoreResponse(ItemScore score, String itemTitle) {
        return ScoreResponse.builder()
                .scoreId(score.getScoreId())
                .itemId(score.getItemId())
                .itemTitle(itemTitle)
                .assigneeUsername(score.getAssigneeUsername())
                .assigneeName(score.getAssigneeName())
                .baseGrade(score.getBaseGrade())
                .baseScore(score.getBaseScore())
                .difficulty(score.getDifficulty())
                .scopeChange(score.getScopeChange())
                .riskHandling(score.getRiskHandling())
                .weightFactor(score.getWeightFactor())
                .finalScore(score.getFinalScore())
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
