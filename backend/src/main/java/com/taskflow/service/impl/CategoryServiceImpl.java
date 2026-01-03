package com.taskflow.service.impl;

import com.taskflow.domain.Category;
import com.taskflow.dto.category.CategoryCreateRequest;
import com.taskflow.dto.category.CategoryResponse;
import com.taskflow.dto.category.CategoryUpdateRequest;
import com.taskflow.exception.BusinessException;
import com.taskflow.mapper.CategoryMapper;
import com.taskflow.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 카테고리 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;

    @Override
    public List<CategoryResponse> getCategories() {
        log.debug("Fetching all active categories");
        return categoryMapper.findAll().stream()
                .map(CategoryResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    public List<CategoryResponse> getCategoriesIncludingDeleted(String useYn) {
        log.debug("Fetching all categories including deleted, useYn={}", useYn);
        return categoryMapper.findAllIncludingDeleted(useYn).stream()
                .map(CategoryResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    public CategoryResponse getCategory(Long categoryId) {
        log.debug("Fetching category: id={}", categoryId);
        Category category = categoryMapper.findById(categoryId)
                .orElseThrow(() -> BusinessException.notFound("카테고리를 찾을 수 없습니다: " + categoryId));
        return CategoryResponse.from(category);
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryCreateRequest request, String createdBy) {
        log.info("Creating category: name={}", request.getCategoryName());

        // 이름 중복 확인
        if (categoryMapper.existsByName(request.getCategoryName())) {
            throw BusinessException.conflict("이미 존재하는 카테고리명입니다: " + request.getCategoryName());
        }

        // 정렬 순서 설정
        Integer sortOrder = request.getSortOrder();
        if (sortOrder == null || sortOrder <= 0) {
            sortOrder = categoryMapper.getMaxSortOrder() + 1;
        }

        // 카테고리 생성
        Category category = Category.builder()
                .categoryName(request.getCategoryName())
                .color(request.getColor() != null ? request.getColor() : "#6B7280")
                .sortOrder(sortOrder)
                .useYn("Y")
                .createdBy(createdBy)
                .build();

        categoryMapper.insert(category);
        log.info("Category created: id={}, name={}", category.getCategoryId(), category.getCategoryName());

        return getCategory(category.getCategoryId());
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long categoryId, CategoryUpdateRequest request, String updatedBy) {
        log.info("Updating category: id={}", categoryId);

        // 카테고리 존재 확인
        Category category = categoryMapper.findById(categoryId)
                .orElseThrow(() -> BusinessException.notFound("카테고리를 찾을 수 없습니다: " + categoryId));

        // 이름 변경 시 중복 확인
        if (request.getCategoryName() != null && !request.getCategoryName().equals(category.getCategoryName())) {
            if (categoryMapper.existsByNameAndIdNot(request.getCategoryName(), categoryId)) {
                throw BusinessException.conflict("이미 존재하는 카테고리명입니다: " + request.getCategoryName());
            }
            category.setCategoryName(request.getCategoryName());
        }

        // 나머지 필드 업데이트
        if (request.getColor() != null) {
            category.setColor(request.getColor());
        }
        if (request.getSortOrder() != null) {
            category.setSortOrder(request.getSortOrder());
        }
        if (request.getUseYn() != null) {
            category.setUseYn(request.getUseYn());
        }
        category.setUpdatedBy(updatedBy);

        categoryMapper.update(category);
        log.info("Category updated: id={}", categoryId);

        return getCategory(categoryId);
    }

    @Override
    @Transactional
    public void deleteCategory(Long categoryId, String deletedBy) {
        log.info("Deleting category: id={}", categoryId);

        // 카테고리 존재 확인
        Category category = categoryMapper.findById(categoryId)
                .orElseThrow(() -> BusinessException.notFound("카테고리를 찾을 수 없습니다: " + categoryId));

        // 사용 중인지 확인 (경고만, 삭제는 허용)
        if (categoryMapper.hasItems(categoryId)) {
            log.warn("Category {} is being used by items, proceeding with soft delete", categoryId);
        }

        // 논리 삭제
        categoryMapper.softDelete(categoryId, deletedBy);
        log.info("Category soft deleted: id={}", categoryId);
    }
}
