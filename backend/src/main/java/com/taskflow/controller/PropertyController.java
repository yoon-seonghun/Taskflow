package com.taskflow.controller;

import com.taskflow.common.ApiResponse;
import com.taskflow.dto.property.*;
import com.taskflow.security.SecurityUtils;
import com.taskflow.service.PropertyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 속성 정의 컨트롤러
 *
 * API:
 * - GET /api/boards/{boardId}/properties - 보드별 속성 정의 목록
 * - POST /api/boards/{boardId}/properties - 보드 속성 정의 생성
 * - GET /api/properties/{id} - 속성 정의 조회
 * - PUT /api/properties/{id} - 속성 정의 수정
 * - DELETE /api/properties/{id} - 속성 정의 삭제
 * - GET /api/properties/{id}/options - 옵션 목록
 * - POST /api/properties/{id}/options - 옵션 추가
 *
 * v2.0 추가 API:
 * - GET /api/properties/global - 글로벌 속성 목록
 * - POST /api/properties/global - 글로벌 속성 생성 (관리자 전용)
 * - GET /api/properties/manager - 매니저 속성 목록
 * - POST /api/properties/manager - 매니저 속성 생성
 * - GET /api/properties/user - 사용자 속성 목록 (본인 생성)
 * - POST /api/properties/user - 사용자 속성 생성 (개인 속성)
 * - GET /api/properties/accessible - 사용자 접근 가능 속성 목록 (글로벌 + 매니저 + 본인)
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService propertyService;

    // =============================================
    // 속성 정의 CRUD
    // =============================================

    // v2.0: 보드별 속성 조회는 BoardController.getBoardProperties()로 이동
    // GET /boards/{boardId}/properties -> BoardController 담당

    /**
     * 속성 정의 생성 (레거시 API - v2.0 호환)
     * 내부적으로 USER 타입 속성을 생성하고 해당 보드에 연결
     */
    @PostMapping("/boards/{boardId}/properties")
    public ResponseEntity<ApiResponse<PropertyResponse>> createProperty(
            @PathVariable("boardId") Long boardId,
            @Valid @RequestBody PropertyCreateRequest request
    ) {
        log.info("Create property: boardId={}, name={}", boardId, request.getPropertyName());

        String currentUsername = SecurityUtils.getCurrentUsername();
        PropertyResponse response = propertyService.createProperty(boardId, request, currentUsername);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "속성이 생성되었습니다"));
    }

    /**
     * 속성 정의 조회
     */
    @GetMapping("/properties/{id}")
    public ResponseEntity<ApiResponse<PropertyResponse>> getProperty(
            @PathVariable("id") Long propertyId
    ) {
        log.debug("Get property: id={}", propertyId);

        PropertyResponse response = propertyService.getProperty(propertyId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 속성 정의 수정
     */
    @PutMapping("/properties/{id}")
    public ResponseEntity<ApiResponse<PropertyResponse>> updateProperty(
            @PathVariable("id") Long propertyId,
            @Valid @RequestBody PropertyUpdateRequest request
    ) {
        log.info("Update property: id={}", propertyId);

        String currentUsername = SecurityUtils.getCurrentUsername();
        PropertyResponse response = propertyService.updateProperty(propertyId, request, currentUsername);

        return ResponseEntity.ok(ApiResponse.success(response, "속성이 수정되었습니다"));
    }

    /**
     * 속성 정의 논리 삭제
     */
    @DeleteMapping("/properties/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProperty(
            @PathVariable("id") Long propertyId
    ) {
        log.info("Soft delete property: id={}", propertyId);

        String currentUsername = SecurityUtils.getCurrentUsername();
        propertyService.deleteProperty(propertyId, currentUsername);
        return ResponseEntity.ok(ApiResponse.successWithMessage("속성이 삭제되었습니다"));
    }

    // =============================================
    // v2.0: 소유 유형별 속성 API
    // =============================================

    /**
     * 글로벌 속성 목록 조회
     */
    @GetMapping("/properties/global")
    public ResponseEntity<ApiResponse<List<PropertyResponse>>> getGlobalProperties() {
        log.debug("Get global properties");

        List<PropertyResponse> response = propertyService.getGlobalProperties();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 글로벌 속성 생성 (관리자 전용)
     */
    @PostMapping("/properties/global")
    public ResponseEntity<ApiResponse<PropertyResponse>> createGlobalProperty(
            @Valid @RequestBody PropertyCreateRequest request
    ) {
        log.info("Create global property: name={}", request.getPropertyName());

        String currentUsername = SecurityUtils.getCurrentUsername();
        PropertyResponse response = propertyService.createGlobalProperty(request, currentUsername);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "글로벌 속성이 생성되었습니다"));
    }

    /**
     * 매니저 속성 목록 조회 (본인 소유 + 상위 부서 속성)
     */
    @GetMapping("/properties/manager")
    public ResponseEntity<ApiResponse<List<PropertyResponse>>> getManagerProperties() {
        log.debug("Get manager properties");

        String currentUsername = SecurityUtils.getCurrentUsername();
        List<PropertyResponse> response = propertyService.getManagerProperties(currentUsername);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 매니저 속성 생성
     */
    @PostMapping("/properties/manager")
    public ResponseEntity<ApiResponse<PropertyResponse>> createManagerProperty(
            @Valid @RequestBody PropertyCreateRequest request
    ) {
        log.info("Create manager property: name={}", request.getPropertyName());

        String currentUsername = SecurityUtils.getCurrentUsername();
        PropertyResponse response = propertyService.createManagerProperty(request, currentUsername);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "매니저 속성이 생성되었습니다"));
    }

    /**
     * 사용자 속성 목록 조회 (본인이 생성한 속성)
     */
    @GetMapping("/properties/user")
    public ResponseEntity<ApiResponse<List<PropertyResponse>>> getUserProperties() {
        log.debug("Get user properties");

        String currentUsername = SecurityUtils.getCurrentUsername();
        List<PropertyResponse> response = propertyService.getUserProperties(currentUsername);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 사용자 속성 생성 (개인 속성)
     * - 개인이 생성하는 속성
     * - 카테고리에 그룹화되어 보드/업무에 활용됨
     */
    @PostMapping("/properties/user")
    public ResponseEntity<ApiResponse<PropertyResponse>> createUserProperty(
            @Valid @RequestBody PropertyCreateRequest request
    ) {
        log.info("Create user property: name={}", request.getPropertyName());

        String currentUsername = SecurityUtils.getCurrentUsername();
        PropertyResponse response = propertyService.createUserProperty(request, currentUsername);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "사용자 속성이 생성되었습니다"));
    }

    /**
     * 사용자가 접근 가능한 모든 속성 조회 (글로벌 + 매니저 + 본인 속성)
     * 카테고리/보드에서 속성 선택 시 사용
     */
    @GetMapping("/properties/accessible")
    public ResponseEntity<ApiResponse<List<PropertyResponse>>> getAccessibleProperties() {
        log.debug("Get accessible properties");

        String currentUsername = SecurityUtils.getCurrentUsername();
        List<PropertyResponse> response = propertyService.getAccessibleProperties(currentUsername);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // =============================================
    // 옵션 (속성 컨트롤러 내 포함)
    // =============================================

    /**
     * 속성별 옵션 목록 조회
     */
    @GetMapping("/properties/{propId}/options")
    public ResponseEntity<ApiResponse<List<OptionDetailResponse>>> getOptionsByPropertyId(
            @PathVariable("propId") Long propertyId,
            @RequestParam(value = "useYn", required = false) String useYn
    ) {
        log.debug("Get options: propertyId={}, useYn={}", propertyId, useYn);

        List<OptionDetailResponse> response = propertyService.getOptionsByPropertyId(propertyId, useYn);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 옵션 추가
     */
    @PostMapping("/properties/{propId}/options")
    public ResponseEntity<ApiResponse<OptionDetailResponse>> createOption(
            @PathVariable("propId") Long propertyId,
            @Valid @RequestBody OptionCreateRequest request
    ) {
        log.info("Create option: propertyId={}, name={}", propertyId, request.getOptionName());

        String currentUsername = SecurityUtils.getCurrentUsername();
        OptionDetailResponse response = propertyService.createOption(propertyId, request, currentUsername);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "옵션이 추가되었습니다"));
    }

    // =============================================
    // v2.0: 속성 이관 API
    // =============================================

    /**
     * 속성 소유권 이전
     * - 속성의 소유자를 다른 사용자로 변경
     *
     * @param propertyId       속성 ID
     * @param newOwnerUsername 새 소유자 USERNAME
     */
    @PostMapping("/properties/{id}/transfer")
    public ResponseEntity<ApiResponse<PropertyResponse>> transferProperty(
            @PathVariable("id") Long propertyId,
            @RequestParam("newOwner") String newOwnerUsername
    ) {
        log.info("Transfer property: id={}, newOwner={}", propertyId, newOwnerUsername);

        String currentUsername = SecurityUtils.getCurrentUsername();
        PropertyResponse response = propertyService.transferProperty(propertyId, newOwnerUsername, currentUsername);

        return ResponseEntity.ok(ApiResponse.success(response, "속성이 이전되었습니다"));
    }

    /**
     * 속성 복사 (다른 사용자에게)
     * - 속성 정의와 옵션을 복사하여 새 소유자에게 제공
     *
     * @param propertyId       원본 속성 ID
     * @param newOwnerUsername 새 소유자 USERNAME
     */
    @PostMapping("/properties/{id}/copy")
    public ResponseEntity<ApiResponse<PropertyResponse>> copyPropertyToUser(
            @PathVariable("id") Long propertyId,
            @RequestParam("newOwner") String newOwnerUsername
    ) {
        log.info("Copy property to user: id={}, newOwner={}", propertyId, newOwnerUsername);

        String currentUsername = SecurityUtils.getCurrentUsername();
        PropertyResponse response = propertyService.copyPropertyToUser(propertyId, newOwnerUsername, currentUsername);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "속성이 복사되었습니다"));
    }

    /**
     * 속성 일괄 이전
     * - 여러 속성을 한 번에 다른 사용자에게 이전
     *
     * @param propertyIds      속성 ID 목록
     * @param newOwnerUsername 새 소유자 USERNAME
     */
    @PostMapping("/properties/transfer-batch")
    public ResponseEntity<ApiResponse<List<PropertyResponse>>> transferProperties(
            @RequestBody List<Long> propertyIds,
            @RequestParam("newOwner") String newOwnerUsername
    ) {
        log.info("Batch transfer properties: count={}, newOwner={}", propertyIds.size(), newOwnerUsername);

        String currentUsername = SecurityUtils.getCurrentUsername();
        List<PropertyResponse> response = propertyService.transferProperties(propertyIds, newOwnerUsername, currentUsername);

        return ResponseEntity.ok(ApiResponse.success(response, response.size() + "개 속성이 이전되었습니다"));
    }
}
