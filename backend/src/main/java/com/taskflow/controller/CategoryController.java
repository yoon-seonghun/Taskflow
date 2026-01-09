package com.taskflow.controller;

import com.taskflow.common.ApiResponse;
import com.taskflow.dto.category.*;
import com.taskflow.security.UserPrincipal;
import com.taskflow.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 카테고리 컨트롤러
 *
 * v2.0 변경사항:
 * - 소유자 기반 카테고리 관리
 * - 카테고리 공유 API
 * - 카테고리 속성 관리 API
 */
@Slf4j
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    // =============================================
    // 카테고리 조회
    // =============================================

    /**
     * 사용 가능한 카테고리 목록 조회 (소유 + 공유받은)
     */
    @GetMapping("/accessible")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAccessibleCategories(
            @AuthenticationPrincipal UserPrincipal principal) {

        log.debug("GET /api/categories/accessible - user={}", principal.getUsername());

        List<CategoryResponse> categories = categoryService.getAccessibleCategories(
                principal.getUsername(), principal.getDepartmentCode());
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    /**
     * 내가 소유한 카테고리 목록 조회
     */
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getMyCategories(
            @AuthenticationPrincipal UserPrincipal principal) {

        log.debug("GET /api/categories/my - user={}", principal.getUsername());

        List<CategoryResponse> categories = categoryService.getMyCateogries(principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    /**
     * 공유받은 카테고리 목록 조회
     */
    @GetMapping("/shared")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getSharedCategories(
            @AuthenticationPrincipal UserPrincipal principal) {

        log.debug("GET /api/categories/shared - user={}", principal.getUsername());

        List<CategoryResponse> categories = categoryService.getSharedWithMeCategories(
                principal.getUsername(), principal.getDepartmentCode());
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    /**
     * 전체 카테고리 목록 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategories(
            @RequestParam(required = false, defaultValue = "false") boolean includeDeleted) {

        log.debug("GET /api/categories - includeDeleted={}", includeDeleted);

        List<CategoryResponse> categories;
        if (includeDeleted) {
            categories = categoryService.getCategoriesIncludingDeleted(null);
        } else {
            categories = categoryService.getCategories();
        }

        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    /**
     * 카테고리 단건 조회
     */
    @GetMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategory(
            @PathVariable Long categoryId,
            @RequestParam(required = false, defaultValue = "false") boolean withDetails) {

        log.debug("GET /api/categories/{} - withDetails={}", categoryId, withDetails);

        CategoryResponse category;
        if (withDetails) {
            category = categoryService.getCategoryWithDetails(categoryId);
        } else {
            category = categoryService.getCategory(categoryId);
        }
        return ResponseEntity.ok(ApiResponse.success(category));
    }

    // =============================================
    // 카테고리 생성/수정/삭제
    // =============================================

    /**
     * 카테고리 생성
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CategoryCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        log.info("POST /api/categories - name={}", request.getCategoryName());

        CategoryResponse category = categoryService.createCategory(request, principal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(category));
    }

    /**
     * 카테고리 수정
     */
    @PutMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        log.info("PUT /api/categories/{}", categoryId);

        CategoryResponse category = categoryService.updateCategory(categoryId, request, principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success(category));
    }

    /**
     * 카테고리 삭제 (논리 삭제)
     */
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable Long categoryId,
            @AuthenticationPrincipal UserPrincipal principal) {

        log.info("DELETE /api/categories/{}", categoryId);

        categoryService.deleteCategory(categoryId, principal.getUsername());
        return ResponseEntity.noContent().build();
    }

    // =============================================
    // 카테고리 공유
    // =============================================

    /**
     * 카테고리 공유 추가
     */
    @PostMapping("/{categoryId}/shares")
    public ResponseEntity<ApiResponse<Void>> addShare(
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryShareRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        log.info("POST /api/categories/{}/shares - type={}, target={}",
                categoryId, request.getShareType(), request.getShareTarget());

        categoryService.addShare(categoryId, request, principal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(null));
    }

    /**
     * 카테고리 공유 일괄 추가
     */
    @PostMapping("/{categoryId}/shares/bulk")
    public ResponseEntity<ApiResponse<Void>> addShares(
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryShareRequest.BulkShareRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        log.info("POST /api/categories/{}/shares/bulk - count={}", categoryId, request.getShares().size());

        categoryService.addShares(categoryId, request, principal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(null));
    }

    /**
     * 카테고리 공유 삭제
     */
    @DeleteMapping("/{categoryId}/shares/{shareId}")
    public ResponseEntity<Void> removeShare(
            @PathVariable Long categoryId,
            @PathVariable Long shareId) {

        log.info("DELETE /api/categories/{}/shares/{}", categoryId, shareId);

        categoryService.removeShare(categoryId, shareId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 카테고리 공유 권한 변경
     */
    @PatchMapping("/shares/{shareId}/permission")
    public ResponseEntity<ApiResponse<Void>> updateSharePermission(
            @PathVariable Long shareId,
            @RequestParam String permission,
            @AuthenticationPrincipal UserPrincipal principal) {

        log.info("PATCH /api/categories/shares/{}/permission - permission={}", shareId, permission);

        categoryService.updateSharePermission(shareId, permission, principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // =============================================
    // 카테고리 속성 관리
    // =============================================

    /**
     * 카테고리별 속성 목록 조회
     */
    @GetMapping("/{categoryId}/properties")
    public ResponseEntity<ApiResponse<List<CategoryPropertyResponse>>> getCategoryProperties(
            @PathVariable Long categoryId) {

        log.debug("GET /api/categories/{}/properties", categoryId);

        List<CategoryPropertyResponse> properties = categoryService.getCategoryProperties(categoryId);
        return ResponseEntity.ok(ApiResponse.success(properties));
    }

    /**
     * 카테고리에 속성 추가
     */
    @PostMapping("/{categoryId}/properties")
    public ResponseEntity<ApiResponse<Void>> addProperty(
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryPropertyRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        log.info("POST /api/categories/{}/properties - propertyId={}", categoryId, request.getPropertyId());

        categoryService.addProperty(categoryId, request, principal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(null));
    }

    /**
     * 카테고리에 속성 일괄 추가
     */
    @PostMapping("/{categoryId}/properties/bulk")
    public ResponseEntity<ApiResponse<Void>> addProperties(
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryPropertyRequest.BulkAddRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        log.info("POST /api/categories/{}/properties/bulk - count={}", categoryId, request.getPropertyIds().size());

        categoryService.addProperties(categoryId, request, principal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(null));
    }

    /**
     * 카테고리에서 속성 제거
     */
    @DeleteMapping("/{categoryId}/properties/{propertyId}")
    public ResponseEntity<Void> removeProperty(
            @PathVariable Long categoryId,
            @PathVariable Long propertyId) {

        log.info("DELETE /api/categories/{}/properties/{}", categoryId, propertyId);

        categoryService.removeProperty(categoryId, propertyId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 카테고리 속성 순서 변경
     */
    @PatchMapping("/{categoryId}/properties/order")
    public ResponseEntity<ApiResponse<Void>> updatePropertyOrder(
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryPropertyRequest.OrderUpdateRequest request) {

        log.info("PATCH /api/categories/{}/properties/order", categoryId);

        categoryService.updatePropertyOrder(categoryId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 카테고리 속성 기본값 수정
     */
    @PatchMapping("/{categoryId}/properties/{propertyId}/default-value")
    public ResponseEntity<ApiResponse<Void>> updatePropertyDefaultValue(
            @PathVariable Long categoryId,
            @PathVariable Long propertyId,
            @Valid @RequestBody CategoryPropertyRequest.DefaultValueUpdateRequest request) {

        log.info("PATCH /api/categories/{}/properties/{}/default-value", categoryId, propertyId);

        categoryService.updatePropertyDefaultValue(categoryId, propertyId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 카테고리 속성 필수 여부 수정 (v2.0)
     */
    @PatchMapping("/{categoryId}/properties/{propertyId}/required")
    public ResponseEntity<ApiResponse<Void>> updatePropertyRequiredYn(
            @PathVariable Long categoryId,
            @PathVariable Long propertyId,
            @Valid @RequestBody CategoryPropertyRequest.RequiredYnUpdateRequest request) {

        log.info("PATCH /api/categories/{}/properties/{}/required - requiredYn={}",
                categoryId, propertyId, request.getRequiredYn());

        categoryService.updatePropertyRequiredYn(categoryId, propertyId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // =============================================
    // v2.0: 카테고리 이관 API
    // =============================================

    /**
     * 카테고리 소유권 이전
     * - 카테고리의 소유자를 다른 사용자로 변경
     *
     * @param categoryId       카테고리 ID
     * @param newOwnerUsername 새 소유자 USERNAME
     */
    @PostMapping("/{categoryId}/transfer")
    public ResponseEntity<ApiResponse<CategoryResponse>> transferCategory(
            @PathVariable Long categoryId,
            @RequestParam("newOwner") String newOwnerUsername,
            @AuthenticationPrincipal UserPrincipal principal) {

        log.info("POST /api/categories/{}/transfer - newOwner={}", categoryId, newOwnerUsername);

        CategoryResponse response = categoryService.transferCategory(
                categoryId, newOwnerUsername, principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response, "카테고리가 이전되었습니다"));
    }

    /**
     * 카테고리 복사 (다른 사용자에게)
     * - 카테고리 정의와 속성 연결을 복사하여 새 소유자에게 제공
     *
     * @param categoryId       원본 카테고리 ID
     * @param newOwnerUsername 새 소유자 USERNAME
     */
    @PostMapping("/{categoryId}/copy")
    public ResponseEntity<ApiResponse<CategoryResponse>> copyCategoryToUser(
            @PathVariable Long categoryId,
            @RequestParam("newOwner") String newOwnerUsername,
            @AuthenticationPrincipal UserPrincipal principal) {

        log.info("POST /api/categories/{}/copy - newOwner={}", categoryId, newOwnerUsername);

        CategoryResponse response = categoryService.copyCategoryToUser(
                categoryId, newOwnerUsername, principal.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "카테고리가 복사되었습니다"));
    }

    /**
     * 카테고리 일괄 이전
     * - 여러 카테고리를 한 번에 다른 사용자에게 이전
     *
     * @param categoryIds      카테고리 ID 목록
     * @param newOwnerUsername 새 소유자 USERNAME
     */
    @PostMapping("/transfer-batch")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> transferCategories(
            @RequestBody List<Long> categoryIds,
            @RequestParam("newOwner") String newOwnerUsername,
            @AuthenticationPrincipal UserPrincipal principal) {

        log.info("POST /api/categories/transfer-batch - count={}, newOwner={}",
                categoryIds.size(), newOwnerUsername);

        List<CategoryResponse> response = categoryService.transferCategories(
                categoryIds, newOwnerUsername, principal.getUsername());
        return ResponseEntity.ok(ApiResponse.success(response, response.size() + "개 카테고리가 이전되었습니다"));
    }
}
