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
 * - GET /api/boards/{boardId}/properties - 속성 정의 목록
 * - POST /api/boards/{boardId}/properties - 속성 정의 생성
 * - GET /api/properties/{id} - 속성 정의 조회
 * - PUT /api/properties/{id} - 속성 정의 수정
 * - DELETE /api/properties/{id} - 속성 정의 삭제
 * - GET /api/properties/{id}/options - 옵션 목록
 * - POST /api/properties/{id}/options - 옵션 추가
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

    /**
     * 보드별 속성 정의 목록 조회
     */
    @GetMapping("/boards/{boardId}/properties")
    public ResponseEntity<ApiResponse<List<PropertyResponse>>> getPropertiesByBoardId(
            @PathVariable("boardId") Long boardId,
            @RequestParam(value = "useYn", required = false) String useYn,
            @RequestParam(value = "cached", required = false, defaultValue = "false") boolean cached
    ) {
        log.debug("Get properties: boardId={}, useYn={}, cached={}", boardId, useYn, cached);

        List<PropertyResponse> response;
        if (cached && (useYn == null || "Y".equals(useYn))) {
            // 캐시 사용 (활성화된 속성만)
            response = propertyService.getCachedPropertiesByBoardId(boardId);
        } else {
            response = propertyService.getPropertiesByBoardId(boardId, useYn);
        }

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 속성 정의 생성
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
}
