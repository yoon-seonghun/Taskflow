package com.taskflow.controller;

import com.taskflow.common.ApiResponse;
import com.taskflow.dto.external.*;
import com.taskflow.security.UserPrincipal;
import com.taskflow.service.ExternalQueryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 외부 쿼리 관리 API
 */
@Slf4j
@RestController
@RequestMapping("/api/external-queries")
@RequiredArgsConstructor
public class ExternalQueryController {

    private final ExternalQueryService queryService;

    // =============================================
    // 글로벌 쿼리 (ADMIN)
    // =============================================

    /**
     * 글로벌 쿼리 목록 조회
     */
    @GetMapping("/global")
    public ResponseEntity<ApiResponse<List<ExternalQueryResponse>>> findGlobalQueries() {
        return ResponseEntity.ok(ApiResponse.success(queryService.findGlobalQueries()));
    }

    /**
     * 글로벌 쿼리 생성 (ADMIN)
     */
    @PostMapping("/global")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Long>> createGlobalQuery(
            @Valid @RequestBody ExternalQueryCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        Long id = queryService.createGlobalQuery(request, principal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(id, "글로벌 쿼리가 생성되었습니다"));
    }

    // =============================================
    // 매니저 쿼리 (MANAGER+)
    // =============================================

    /**
     * 매니저 쿼리 목록 조회
     */
    @GetMapping("/manager")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<ExternalQueryResponse>>> findManagerQueries(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(queryService.findManagerQueries(principal.getUsername())));
    }

    /**
     * 매니저 쿼리 생성
     */
    @PostMapping("/manager")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Long>> createManagerQuery(
            @Valid @RequestBody ExternalQueryCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        Long id = queryService.createManagerQuery(request, principal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(id, "매니저 쿼리가 생성되었습니다"));
    }

    // =============================================
    // 사용자 쿼리 (USER)
    // =============================================

    /**
     * 사용자 쿼리 목록 조회
     */
    @GetMapping("/user")
    public ResponseEntity<ApiResponse<List<ExternalQueryResponse>>> findUserQueries(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(queryService.findUserQueries(principal.getUsername())));
    }

    /**
     * 사용자 쿼리 생성
     */
    @PostMapping("/user")
    public ResponseEntity<ApiResponse<Long>> createUserQuery(
            @Valid @RequestBody ExternalQueryCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        Long id = queryService.createUserQuery(request, principal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(id, "사용자 쿼리가 생성되었습니다"));
    }

    // =============================================
    // 접근 가능 전체 쿼리
    // =============================================

    /**
     * 접근 가능한 전체 쿼리 목록 조회
     */
    @GetMapping("/accessible")
    public ResponseEntity<ApiResponse<List<ExternalQueryResponse>>> findAccessibleQueries(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(queryService.findAccessibleQueries(principal.getUsername())));
    }

    // =============================================
    // 개별 쿼리 CRUD
    // =============================================

    /**
     * 쿼리 상세 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ExternalQueryResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(queryService.findById(id)));
    }

    /**
     * 쿼리 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> update(
            @PathVariable Long id,
            @Valid @RequestBody ExternalQueryUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        // 권한 확인
        if (!queryService.canModify(id, principal.getUsername())) {
            // ADMIN은 모든 쿼리 수정 가능
            if (!principal.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("쿼리 수정 권한이 없습니다"));
            }
        }

        queryService.update(id, request, principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success(null, "쿼리가 수정되었습니다"));
    }

    /**
     * 쿼리 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        // 권한 확인
        if (!queryService.canModify(id, principal.getUsername())) {
            if (!principal.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("쿼리 삭제 권한이 없습니다"));
            }
        }

        queryService.delete(id, principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success(null, "쿼리가 삭제되었습니다"));
    }

    // =============================================
    // 쿼리 테스트/실행
    // =============================================

    /**
     * 쿼리 테스트 (저장 전)
     */
    @PostMapping("/test")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<QueryTestResponse>> testQuery(
            @Valid @RequestBody QueryTestRequest request) {

        QueryTestResponse result = queryService.testQuery(request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 쿼리 실행 (옵션 조회)
     */
    @GetMapping("/{id}/execute")
    public ResponseEntity<ApiResponse<QueryExecuteResponse>> executeQuery(@PathVariable Long id) {
        QueryExecuteResponse result = queryService.executeQuery(id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 쿼리 실행 (캐시 무시)
     */
    @PostMapping("/{id}/execute")
    public ResponseEntity<ApiResponse<QueryExecuteResponse>> executeQueryNoCache(@PathVariable Long id) {
        QueryExecuteResponse result = queryService.executeQueryNoCache(id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    // =============================================
    // 캐시 관리
    // =============================================

    /**
     * 쿼리 캐시 무효화
     */
    @DeleteMapping("/{id}/cache")
    public ResponseEntity<ApiResponse<Void>> evictCache(@PathVariable Long id) {
        queryService.evictCache(id);
        return ResponseEntity.ok(ApiResponse.success(null, "캐시가 무효화되었습니다"));
    }

    // =============================================
    // 데이터소스별 쿼리 조회
    // =============================================

    /**
     * 데이터소스별 쿼리 목록 조회
     */
    @GetMapping("/datasource/{datasourceId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ExternalQueryResponse>>> findByDatasourceId(
            @PathVariable Long datasourceId) {
        return ResponseEntity.ok(ApiResponse.success(queryService.findByDatasourceId(datasourceId)));
    }

    /**
     * 키워드 검색
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ExternalQueryResponse>>> searchByKeyword(
            @RequestParam String keyword) {
        return ResponseEntity.ok(ApiResponse.success(queryService.searchByKeyword(keyword)));
    }
}
