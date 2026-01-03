package com.taskflow.service;

import com.taskflow.dto.category.CategoryCreateRequest;
import com.taskflow.dto.category.CategoryResponse;
import com.taskflow.dto.category.CategoryUpdateRequest;

import java.util.List;

/**
 * 카테고리 서비스 인터페이스
 */
public interface CategoryService {

    /**
     * 전체 카테고리 목록 조회 (활성만)
     *
     * @return 카테고리 목록
     */
    List<CategoryResponse> getCategories();

    /**
     * 전체 카테고리 목록 조회 (삭제 포함)
     *
     * @param useYn 사용 여부 필터 (null = 전체)
     * @return 카테고리 목록
     */
    List<CategoryResponse> getCategoriesIncludingDeleted(String useYn);

    /**
     * 카테고리 단건 조회
     *
     * @param categoryId 카테고리 ID
     * @return 카테고리 정보
     */
    CategoryResponse getCategory(Long categoryId);

    /**
     * 카테고리 생성
     *
     * @param request   생성 요청
     * @param createdBy 생성자 username
     * @return 생성된 카테고리 정보
     */
    CategoryResponse createCategory(CategoryCreateRequest request, String createdBy);

    /**
     * 카테고리 수정
     *
     * @param categoryId 카테고리 ID
     * @param request    수정 요청
     * @param updatedBy  수정자 username
     * @return 수정된 카테고리 정보
     */
    CategoryResponse updateCategory(Long categoryId, CategoryUpdateRequest request, String updatedBy);

    /**
     * 카테고리 삭제 (논리 삭제)
     *
     * @param categoryId 카테고리 ID
     * @param deletedBy  삭제자 username
     */
    void deleteCategory(Long categoryId, String deletedBy);
}
