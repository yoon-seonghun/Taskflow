package com.taskflow.controller;

import com.taskflow.common.ApiResponse;
import com.taskflow.dto.position.*;
import com.taskflow.security.SecurityUtils;
import com.taskflow.service.PositionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 직급 컨트롤러
 *
 * 권한: 모든 인증된 사용자 (향후 ADMIN 역할로 제한 가능)
 *
 * API:
 * - GET /api/positions - 직급 목록
 * - POST /api/positions - 직급 생성
 * - GET /api/positions/{code} - 직급 조회
 * - PUT /api/positions/{code} - 직급 수정
 * - DELETE /api/positions/{code} - 직급 삭제
 * - PUT /api/positions/order - 직급 순서 일괄 변경
 * - GET /api/positions/check-code - 직급 코드 중복 확인
 */
@Slf4j
@RestController
@RequestMapping("/api/positions")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class PositionController {

    private final PositionService positionService;

    /**
     * 직급 목록 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<PositionResponse>>> getPositions(
            @RequestParam(value = "useYn", required = false) String useYn
    ) {
        log.debug("Get positions: useYn={}", useYn);

        List<PositionResponse> response = positionService.getPositions(useYn);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 직급 생성
     */
    @PostMapping
    public ResponseEntity<ApiResponse<PositionResponse>> createPosition(
            @Valid @RequestBody PositionCreateRequest request
    ) {
        log.info("Create position: code={}", request.getPositionCode());

        String currentUsername = SecurityUtils.getCurrentUsername();
        PositionResponse response = positionService.createPosition(request, currentUsername);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "직급이 등록되었습니다"));
    }

    /**
     * 직급 조회 (코드 기반)
     */
    @GetMapping("/{code}")
    public ResponseEntity<ApiResponse<PositionResponse>> getPosition(
            @PathVariable("code") String positionCode
    ) {
        log.debug("Get position: code={}", positionCode);

        PositionResponse response = positionService.getPosition(positionCode);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 직급 수정
     */
    @PutMapping("/{code}")
    public ResponseEntity<ApiResponse<PositionResponse>> updatePosition(
            @PathVariable("code") String positionCode,
            @Valid @RequestBody PositionUpdateRequest request
    ) {
        log.info("Update position: code={}", positionCode);

        String currentUsername = SecurityUtils.getCurrentUsername();
        PositionResponse response = positionService.updatePosition(positionCode, request, currentUsername);

        return ResponseEntity.ok(ApiResponse.success(response, "직급 정보가 수정되었습니다"));
    }

    /**
     * 직급 삭제
     */
    @DeleteMapping("/{code}")
    public ResponseEntity<ApiResponse<Void>> deletePosition(
            @PathVariable("code") String positionCode
    ) {
        log.info("Delete position: code={}", positionCode);

        positionService.deletePosition(positionCode);
        return ResponseEntity.ok(ApiResponse.successWithMessage("직급이 삭제되었습니다"));
    }

    /**
     * 직급 순서 일괄 변경
     */
    @PutMapping("/order")
    public ResponseEntity<ApiResponse<Void>> updatePositionOrder(
            @Valid @RequestBody PositionOrderRequest request
    ) {
        log.info("Update position order: codes={}", request.getPositionCodes());

        String currentUsername = SecurityUtils.getCurrentUsername();
        positionService.updatePositionOrder(request, currentUsername);

        return ResponseEntity.ok(ApiResponse.successWithMessage("직급 순서가 변경되었습니다"));
    }

    /**
     * 직급 코드 중복 확인
     */
    @GetMapping("/check-code")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkCode(
            @RequestParam("code") String positionCode
    ) {
        log.debug("Check position code: {}", positionCode);

        boolean exists = positionService.existsByCode(positionCode);
        return ResponseEntity.ok(ApiResponse.success(Map.of("exists", exists)));
    }

    /**
     * CRUD 활성화 여부 확인
     */
    @GetMapping("/crud-enabled")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> isCrudEnabled() {
        boolean crudEnabled = positionService.isCrudEnabled();
        return ResponseEntity.ok(ApiResponse.success(Map.of("crudEnabled", crudEnabled)));
    }
}
