package com.taskflow.controller;

import com.taskflow.common.ApiResponse;
import com.taskflow.dto.category.CategoryCreateRequest;
import com.taskflow.dto.category.CategoryResponse;
import com.taskflow.dto.category.CategoryUpdateRequest;
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
 * 전역 카테고리 관리 API
 */
@Slf4j
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 카테고리 목록 조회
     *
     * @param includeDeleted 삭제된 항목 포함 여부
     * @return 카테고리 목록
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
     *
     * @param categoryId 카테고리 ID
     * @return 카테고리 정보
     */
    @GetMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategory(
            @PathVariable Long categoryId) {

        log.debug("GET /api/categories/{}", categoryId);

        CategoryResponse category = categoryService.getCategory(categoryId);
        return ResponseEntity.ok(ApiResponse.success(category));
    }

    /**
     * 카테고리 생성
     *
     * @param request   생성 요청
     * @param principal 인증된 사용자
     * @return 생성된 카테고리 정보
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
     *
     * @param categoryId 카테고리 ID
     * @param request    수정 요청
     * @param principal  인증된 사용자
     * @return 수정된 카테고리 정보
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
     *
     * @param categoryId 카테고리 ID
     * @param principal  인증된 사용자
     * @return 204 No Content
     */
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deleteCategory(
            @PathVariable Long categoryId,
            @AuthenticationPrincipal UserPrincipal principal) {

        log.info("DELETE /api/categories/{}", categoryId);

        categoryService.deleteCategory(categoryId, principal.getUsername());
        return ResponseEntity.noContent().build();
    }
}
