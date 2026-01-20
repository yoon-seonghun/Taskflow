package com.taskflow.controller;

import com.taskflow.domain.ItemScore;
import com.taskflow.common.ApiResponse;
import com.taskflow.dto.score.*;
import com.taskflow.security.UserPrincipal;
import com.taskflow.service.ItemScoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 성과점수 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/scores")
@RequiredArgsConstructor
public class ItemScoreController {

    private final ItemScoreService itemScoreService;

    // =============================================
    // 조회
    // =============================================

    /**
     * 점수 상세 조회
     */
    @GetMapping("/{scoreId}")
    public ResponseEntity<ApiResponse<ScoreResponse>> getScore(
            @PathVariable Long scoreId,
            @AuthenticationPrincipal UserPrincipal principal) {

        ItemScore score = itemScoreService.findById(scoreId)
                .orElseThrow(() -> new RuntimeException("점수를 찾을 수 없습니다: " + scoreId));

        // 본인 점수이거나 PM 권한이 있으면 조회 가능
        if (!isOwnerOrPm(score.getAssigneeUsername(), principal)) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("점수 조회 권한이 없습니다"));
        }

        ScoreResponse response = convertToResponse(score);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 업무별 점수 조회
     */
    @GetMapping("/item/{itemId}")
    public ResponseEntity<ApiResponse<ScoreResponse>> getScoreByItemId(
            @PathVariable Long itemId,
            @AuthenticationPrincipal UserPrincipal principal) {

        ItemScore score = itemScoreService.findByItemId(itemId).orElse(null);
        if (score == null) {
            return ResponseEntity.ok(ApiResponse.success(null));
        }

        // 본인 점수이거나 PM 권한이 있으면 조회 가능
        if (!isOwnerOrPm(score.getAssigneeUsername(), principal)) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("점수 조회 권한이 없습니다"));
        }

        ScoreResponse response = convertToResponse(score);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 내 점수 목록 조회
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<ScoreResponse>>> getMyScores(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserPrincipal principal) {

        List<ItemScore> scores = itemScoreService.findByAssignee(
                principal.getUsername(), startDate, endDate);

        List<ScoreResponse> responses = scores.stream()
                .map(this::convertToResponse)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * 내 점수 요약 조회
     */
    @GetMapping("/my/summary")
    public ResponseEntity<ApiResponse<ScoreSummaryResponse>> getMySummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserPrincipal principal) {

        ScoreSummaryResponse summary = itemScoreService.getSummaryByAssignee(
                principal.getUsername(), startDate, endDate);

        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    /**
     * 검색 조건으로 점수 목록 조회 (PM용)
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TEAM_LEADER')")
    public ResponseEntity<ApiResponse<SearchResult<ScoreResponse>>> searchScores(
            @ModelAttribute ScoreSearchRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        // TEAM_LEADER는 자기 부서만 조회 가능
        if (hasRole(principal, "ROLE_TEAM_LEADER") && request.getDeptCode() == null) {
            request.setDeptCode(principal.getDepartmentCode());
        }

        List<ItemScore> scores = itemScoreService.search(request);
        int totalCount = itemScoreService.countBySearch(request);

        List<ScoreResponse> responses = scores.stream()
                .map(this::convertToResponse)
                .toList();

        SearchResult<ScoreResponse> result = new SearchResult<>(responses, totalCount,
                request.getPage(), request.getSize());

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 부서별 점수 요약 목록 조회 (PM용)
     */
    @GetMapping("/department/{deptCode}/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TEAM_LEADER')")
    public ResponseEntity<ApiResponse<List<ScoreSummaryResponse>>> getDepartmentSummary(
            @PathVariable String deptCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @AuthenticationPrincipal UserPrincipal principal) {

        // TEAM_LEADER는 자기 부서만 조회 가능
        if (hasRole(principal, "ROLE_TEAM_LEADER") &&
                !deptCode.equals(principal.getDepartmentCode())) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("다른 부서의 점수를 조회할 권한이 없습니다"));
        }

        List<ScoreSummaryResponse> summaries = itemScoreService.getSummaryByDepartment(
                deptCode, startDate, endDate);

        return ResponseEntity.ok(ApiResponse.success(summaries));
    }

    /**
     * 승인 대기 점수 목록 조회 (PM용)
     */
    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TEAM_LEADER')")
    public ResponseEntity<ApiResponse<List<ScoreResponse>>> getPendingApproval(
            @AuthenticationPrincipal UserPrincipal principal) {

        // TEAM_LEADER는 자기 부서만
        String deptCode = hasRole(principal, "ROLE_TEAM_LEADER") ?
                principal.getDepartmentCode() : null;

        List<ItemScore> scores = itemScoreService.findPendingApproval(deptCode);

        List<ScoreResponse> responses = scores.stream()
                .map(this::convertToResponse)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    // =============================================
    // 점수 계산 및 등록
    // =============================================

    /**
     * 업무 완료 시 점수 계산 및 등록
     */
    @PostMapping("/calculate")
    public ResponseEntity<ApiResponse<ScoreResponse>> calculateScore(
            @Valid @RequestBody ScoreCalculateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        ScoreResponse response = itemScoreService.calculateAndSave(request, principal.getUsername());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // =============================================
    // 수정
    // =============================================

    /**
     * 가중치 요소 수정 (PM용)
     */
    @PutMapping("/{scoreId}/weight")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TEAM_LEADER')")
    public ResponseEntity<ApiResponse<Void>> updateWeightFactors(
            @PathVariable Long scoreId,
            @RequestBody WeightUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        itemScoreService.updateWeightFactors(scoreId, request.difficulty(),
                request.scopeChange(), request.riskHandling(), principal.getUsername());

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // =============================================
    // 승인 처리
    // =============================================

    /**
     * 점수 승인/거부 처리 (PM용)
     */
    @PostMapping("/{scoreId}/approval")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'TEAM_LEADER')")
    public ResponseEntity<ApiResponse<Void>> processApproval(
            @PathVariable Long scoreId,
            @Valid @RequestBody ScoreApprovalRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        itemScoreService.processApproval(scoreId, request, principal.getUsername());

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // =============================================
    // 삭제
    // =============================================

    /**
     * 점수 삭제 (ADMIN 전용)
     */
    @DeleteMapping("/{scoreId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteScore(@PathVariable Long scoreId) {
        itemScoreService.delete(scoreId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // =============================================
    // 헬퍼 메서드
    // =============================================

    private boolean hasRole(UserPrincipal principal, String role) {
        return principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals(role));
    }

    private boolean isPm(UserPrincipal principal) {
        return principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_ADMIN") || a.equals("ROLE_MANAGER") || a.equals("ROLE_TEAM_LEADER"));
    }

    private boolean isOwnerOrPm(String assigneeUsername, UserPrincipal principal) {
        return principal.getUsername().equals(assigneeUsername) || isPm(principal);
    }

    private ScoreResponse convertToResponse(ItemScore score) {
        return ScoreResponse.builder()
                .scoreId(score.getScoreId())
                .itemId(score.getItemId())
                .itemTitle(score.getItemTitle())
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

    // =============================================
    // 내부 클래스
    // =============================================

    /**
     * 검색 결과 래퍼
     */
    public record SearchResult<T>(
            List<T> items,
            int totalCount,
            int page,
            int size
    ) {
        public int getTotalPages() {
            return (int) Math.ceil((double) totalCount / size);
        }
    }

    /**
     * 가중치 수정 요청
     */
    public record WeightUpdateRequest(
            Integer difficulty,
            Integer scopeChange,
            Integer riskHandling
    ) {}
}
