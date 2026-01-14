package com.taskflow.controller;

import com.taskflow.common.ApiResponse;
import com.taskflow.dto.external.ConnectionTestResponse;
import com.taskflow.dto.external.ExternalDatasourceCreateRequest;
import com.taskflow.dto.external.ExternalDatasourceResponse;
import com.taskflow.dto.external.ExternalDatasourceUpdateRequest;
import com.taskflow.security.UserPrincipal;
import com.taskflow.service.ExternalDatasourceService;
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
 * 외부 데이터소스 관리 API (ADMIN 전용)
 */
@Slf4j
@RestController
@RequestMapping("/api/external-datasources")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ExternalDatasourceController {

    private final ExternalDatasourceService datasourceService;

    /**
     * 데이터소스 목록 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ExternalDatasourceResponse>>> findAll(
            @RequestParam(required = false) String dbType,
            @RequestParam(required = false) String keyword) {

        List<ExternalDatasourceResponse> result;
        if (keyword != null && !keyword.isBlank()) {
            result = datasourceService.searchByKeyword(keyword);
        } else if (dbType != null && !dbType.isBlank()) {
            result = datasourceService.findByDbType(dbType);
        } else {
            result = datasourceService.findAll();
        }

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 활성 데이터소스 목록 조회
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<ExternalDatasourceResponse>>> findAllActive() {
        return ResponseEntity.ok(ApiResponse.success(datasourceService.findAllActive()));
    }

    /**
     * 데이터소스 상세 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ExternalDatasourceResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(datasourceService.findById(id)));
    }

    /**
     * 데이터소스 생성
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> create(
            @Valid @RequestBody ExternalDatasourceCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        Long id = datasourceService.create(request, principal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(id, "데이터소스가 생성되었습니다"));
    }

    /**
     * 데이터소스 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> update(
            @PathVariable Long id,
            @Valid @RequestBody ExternalDatasourceUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        datasourceService.update(id, request, principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success(null, "데이터소스가 수정되었습니다"));
    }

    /**
     * 데이터소스 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        datasourceService.delete(id, principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success(null, "데이터소스가 삭제되었습니다"));
    }

    /**
     * 연결 테스트 (기존 데이터소스)
     */
    @PostMapping("/{id}/test-connection")
    public ResponseEntity<ApiResponse<ConnectionTestResponse>> testConnection(@PathVariable Long id) {
        ConnectionTestResponse result = datasourceService.testConnection(id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 연결 테스트 (새 설정)
     */
    @PostMapping("/test-connection")
    public ResponseEntity<ApiResponse<ConnectionTestResponse>> testNewConnection(
            @Valid @RequestBody ExternalDatasourceCreateRequest request) {

        ConnectionTestResponse result = datasourceService.testConnection(request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * DataSource 풀 재초기화
     */
    @PostMapping("/{id}/reinitialize")
    public ResponseEntity<ApiResponse<Void>> reinitializePool(@PathVariable Long id) {
        datasourceService.initializePool(id);
        return ResponseEntity.ok(ApiResponse.success(null, "풀이 재초기화되었습니다"));
    }
}
