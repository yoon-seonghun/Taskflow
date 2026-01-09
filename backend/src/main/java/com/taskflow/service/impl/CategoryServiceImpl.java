package com.taskflow.service.impl;

import com.taskflow.domain.Category;
import com.taskflow.domain.CategoryProperty;
import com.taskflow.domain.CategoryShare;
import com.taskflow.domain.User;
import com.taskflow.dto.category.*;
import com.taskflow.exception.BusinessException;
import com.taskflow.mapper.CategoryMapper;
import com.taskflow.mapper.CategoryPropertyMapper;
import com.taskflow.mapper.CategoryShareMapper;
import com.taskflow.mapper.UserMapper;
import com.taskflow.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 카테고리 서비스 구현체
 *
 * v2.0 변경사항:
 * - 소유자 기반 카테고리 관리
 * - 카테고리 공유 기능
 * - 카테고리-속성 매핑 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;
    private final CategoryShareMapper categoryShareMapper;
    private final CategoryPropertyMapper categoryPropertyMapper;
    private final UserMapper userMapper;

    // =============================================
    // 카테고리 조회
    // =============================================

    @Override
    public List<CategoryResponse> getAccessibleCategories(String username, String deptCode) {
        log.debug("Fetching accessible categories for user={}, deptCode={}", username, deptCode);
        return categoryMapper.findAccessible(username, deptCode).stream()
                .map(CategoryResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    public List<CategoryResponse> getMyCateogries(String ownerUsername) {
        log.debug("Fetching categories owned by user={}", ownerUsername);
        return categoryMapper.findByOwner(ownerUsername).stream()
                .map(CategoryResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    public List<CategoryResponse> getSharedWithMeCategories(String username, String deptCode) {
        log.debug("Fetching categories shared with user={}, deptCode={}", username, deptCode);

        // 사용자에게 직접 공유된 카테고리
        Set<Long> sharedCategoryIds = new HashSet<>();
        List<Category> result = new ArrayList<>();

        List<Category> userShared = categoryMapper.findSharedWithUser(username);
        for (Category c : userShared) {
            if (!sharedCategoryIds.contains(c.getCategoryId())) {
                sharedCategoryIds.add(c.getCategoryId());
                result.add(c);
            }
        }

        // 부서에 공유된 카테고리
        if (deptCode != null && !deptCode.isEmpty()) {
            List<Category> deptShared = categoryMapper.findSharedWithDepartment(deptCode);
            for (Category c : deptShared) {
                if (!sharedCategoryIds.contains(c.getCategoryId())) {
                    sharedCategoryIds.add(c.getCategoryId());
                    result.add(c);
                }
            }
        }

        return result.stream()
                .map(CategoryResponse::from)
                .collect(Collectors.toList());
    }

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
    public CategoryResponse getCategoryWithDetails(Long categoryId) {
        log.debug("Fetching category with details: id={}", categoryId);
        Category category = categoryMapper.findByIdWithDetails(categoryId)
                .orElseThrow(() -> BusinessException.notFound("카테고리를 찾을 수 없습니다: " + categoryId));
        return CategoryResponse.fromWithDetails(category);
    }

    // =============================================
    // 카테고리 생성/수정/삭제
    // =============================================

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryCreateRequest request, String createdBy) {
        log.info("Creating category: name={}, owner={}", request.getCategoryName(), createdBy);

        // 이름 중복 확인
        if (categoryMapper.existsByName(request.getCategoryName())) {
            throw BusinessException.conflict("이미 존재하는 카테고리명입니다: " + request.getCategoryName());
        }

        // 카테고리 코드 처리 (없으면 자동 생성)
        String categoryCode = request.getCategoryCode();
        if (categoryCode == null || categoryCode.trim().isEmpty()) {
            categoryCode = "CAT_" + System.currentTimeMillis();
        }

        // 코드 중복 확인
        if (categoryMapper.existsByCode(categoryCode)) {
            throw BusinessException.conflict("이미 존재하는 카테고리 코드입니다: " + categoryCode);
        }

        // 정렬 순서 설정
        Integer sortOrder = request.getSortOrder();
        if (sortOrder == null || sortOrder <= 0) {
            sortOrder = categoryMapper.getMaxSortOrderByOwner(createdBy) + 1;
        }

        // 카테고리 생성
        Category category = Category.builder()
                .categoryCode(categoryCode)
                .categoryName(request.getCategoryName())
                .description(request.getDescription())
                .categoryColor(request.getCategoryColor() != null ? request.getCategoryColor() : "#6B7280")
                .ownerUsername(createdBy)
                .sortOrder(sortOrder)
                .useYn("Y")
                .createdBy(createdBy)
                .build();

        categoryMapper.insert(category);
        log.info("Category created: id={}, name={}", category.getCategoryId(), category.getCategoryName());

        // 초기 속성 추가 (있는 경우)
        if (request.getPropertyIds() != null && !request.getPropertyIds().isEmpty()) {
            addPropertiesInternal(category.getCategoryId(), request.getPropertyIds(), createdBy);
        }

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

        // 코드 변경 시 중복 확인
        if (request.getCategoryCode() != null && !request.getCategoryCode().equals(category.getCategoryCode())) {
            if (categoryMapper.existsByCodeAndIdNot(request.getCategoryCode(), categoryId)) {
                throw BusinessException.conflict("이미 존재하는 카테고리 코드입니다: " + request.getCategoryCode());
            }
            category.setCategoryCode(request.getCategoryCode());
        }

        // 나머지 필드 업데이트
        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }
        if (request.getCategoryColor() != null) {
            category.setCategoryColor(request.getCategoryColor());
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
        categoryMapper.findById(categoryId)
                .orElseThrow(() -> BusinessException.notFound("카테고리를 찾을 수 없습니다: " + categoryId));

        // 사용 중인지 확인 (경고만, 삭제는 허용)
        if (categoryMapper.hasBoards(categoryId)) {
            log.warn("Category {} is being used by boards, proceeding with soft delete", categoryId);
        }

        // 논리 삭제
        categoryMapper.softDelete(categoryId, deletedBy);
        log.info("Category soft deleted: id={}", categoryId);
    }

    // =============================================
    // 카테고리 공유
    // =============================================

    @Override
    @Transactional
    public void addShare(Long categoryId, CategoryShareRequest request, String createdBy) {
        log.info("Adding share to category: id={}, type={}, target={}",
                categoryId, request.getShareType(), request.getShareTarget());

        // 카테고리 존재 확인
        categoryMapper.findById(categoryId)
                .orElseThrow(() -> BusinessException.notFound("카테고리를 찾을 수 없습니다: " + categoryId));

        // 중복 확인
        if (categoryShareMapper.exists(categoryId, request.getShareType(), request.getShareTarget())) {
            throw BusinessException.conflict("이미 공유된 대상입니다");
        }

        CategoryShare share = CategoryShare.builder()
                .categoryId(categoryId)
                .shareType(request.getShareType())
                .shareTarget(request.getShareTarget())
                .permission(request.getPermission())
                .createdBy(createdBy)
                .build();

        categoryShareMapper.insert(share);
        log.info("Share added: shareId={}", share.getShareId());
    }

    @Override
    @Transactional
    public void addShares(Long categoryId, CategoryShareRequest.BulkShareRequest request, String createdBy) {
        log.info("Adding bulk shares to category: id={}, count={}", categoryId, request.getShares().size());

        for (CategoryShareRequest share : request.getShares()) {
            try {
                addShare(categoryId, share, createdBy);
            } catch (BusinessException e) {
                log.warn("Failed to add share: {}", e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public void removeShare(Long categoryId, Long shareId) {
        log.info("Removing share: categoryId={}, shareId={}", categoryId, shareId);

        CategoryShare share = categoryShareMapper.findById(shareId)
                .orElseThrow(() -> BusinessException.notFound("공유 정보를 찾을 수 없습니다: " + shareId));

        if (!share.getCategoryId().equals(categoryId)) {
            throw BusinessException.badRequest("해당 카테고리의 공유가 아닙니다");
        }

        categoryShareMapper.delete(shareId);
        log.info("Share removed: shareId={}", shareId);
    }

    @Override
    @Transactional
    public void updateSharePermission(Long shareId, String permission, String updatedBy) {
        log.info("Updating share permission: shareId={}, permission={}", shareId, permission);

        categoryShareMapper.findById(shareId)
                .orElseThrow(() -> BusinessException.notFound("공유 정보를 찾을 수 없습니다: " + shareId));

        categoryShareMapper.updatePermission(shareId, permission, updatedBy);
        log.info("Share permission updated: shareId={}", shareId);
    }

    // =============================================
    // 카테고리 속성 관리
    // =============================================

    @Override
    public List<CategoryPropertyResponse> getCategoryProperties(Long categoryId) {
        log.debug("Getting category properties: categoryId={}", categoryId);

        // 카테고리 존재 확인
        categoryMapper.findById(categoryId)
                .orElseThrow(() -> BusinessException.notFound("카테고리를 찾을 수 없습니다: " + categoryId));

        List<CategoryProperty> properties = categoryPropertyMapper.findByCategoryId(categoryId);
        return CategoryPropertyResponse.fromList(properties);
    }

    @Override
    @Transactional
    public void addProperty(Long categoryId, CategoryPropertyRequest request, String createdBy) {
        log.info("Adding property to category: categoryId={}, propertyId={}", categoryId, request.getPropertyId());

        // 카테고리 존재 확인
        categoryMapper.findById(categoryId)
                .orElseThrow(() -> BusinessException.notFound("카테고리를 찾을 수 없습니다: " + categoryId));

        // 중복 확인
        if (categoryPropertyMapper.exists(categoryId, request.getPropertyId())) {
            throw BusinessException.conflict("이미 추가된 속성입니다");
        }

        // 정렬 순서 설정
        Integer sortOrder = request.getSortOrder();
        if (sortOrder == null || sortOrder <= 0) {
            sortOrder = categoryPropertyMapper.getMaxSortOrder(categoryId) + 1;
        }

        CategoryProperty cp = CategoryProperty.builder()
                .categoryId(categoryId)
                .propertyId(request.getPropertyId())
                .sortOrder(sortOrder)
                .defaultValue(request.getDefaultValue())
                .requiredYn(request.getRequiredYn() != null ? request.getRequiredYn() : "N")  // v2.0
                .createdBy(createdBy)
                .build();

        categoryPropertyMapper.insert(cp);
        log.info("Property added to category");
    }

    @Override
    @Transactional
    public void addProperties(Long categoryId, CategoryPropertyRequest.BulkAddRequest request, String createdBy) {
        log.info("Adding bulk properties to category: categoryId={}, count={}", categoryId, request.getPropertyIds().size());
        addPropertiesInternal(categoryId, request.getPropertyIds(), createdBy);
    }

    private void addPropertiesInternal(Long categoryId, List<Long> propertyIds, String createdBy) {
        int sortOrder = categoryPropertyMapper.getMaxSortOrder(categoryId);

        List<CategoryProperty> toInsert = new ArrayList<>();
        for (Long propertyId : propertyIds) {
            if (!categoryPropertyMapper.exists(categoryId, propertyId)) {
                sortOrder++;
                toInsert.add(CategoryProperty.builder()
                        .categoryId(categoryId)
                        .propertyId(propertyId)
                        .sortOrder(sortOrder)
                        .createdBy(createdBy)
                        .build());
            }
        }

        if (!toInsert.isEmpty()) {
            categoryPropertyMapper.insertBatch(toInsert);
            log.info("Added {} properties to category {}", toInsert.size(), categoryId);
        }
    }

    @Override
    @Transactional
    public void removeProperty(Long categoryId, Long propertyId) {
        log.info("Removing property from category: categoryId={}, propertyId={}", categoryId, propertyId);

        if (!categoryPropertyMapper.exists(categoryId, propertyId)) {
            throw BusinessException.notFound("카테고리에 해당 속성이 없습니다");
        }

        categoryPropertyMapper.delete(categoryId, propertyId);
        log.info("Property removed from category");
    }

    @Override
    @Transactional
    public void updatePropertyOrder(Long categoryId, CategoryPropertyRequest.OrderUpdateRequest request) {
        log.info("Updating property order: categoryId={}", categoryId);

        for (CategoryPropertyRequest.OrderUpdateRequest.PropertyOrder order : request.getOrders()) {
            categoryPropertyMapper.updateSortOrder(categoryId, order.getPropertyId(), order.getSortOrder());
        }
        log.info("Property order updated");
    }

    @Override
    @Transactional
    public void updatePropertyDefaultValue(Long categoryId, Long propertyId,
                                           CategoryPropertyRequest.DefaultValueUpdateRequest request) {
        log.info("Updating property default value: categoryId={}, propertyId={}", categoryId, propertyId);

        if (!categoryPropertyMapper.exists(categoryId, propertyId)) {
            throw BusinessException.notFound("카테고리에 해당 속성이 없습니다");
        }

        categoryPropertyMapper.updateDefaultValue(categoryId, propertyId, request.getDefaultValue());
        log.info("Property default value updated");
    }

    @Override
    @Transactional
    public void updatePropertyRequiredYn(Long categoryId, Long propertyId,
                                         CategoryPropertyRequest.RequiredYnUpdateRequest request) {
        log.info("Updating property required status: categoryId={}, propertyId={}, requiredYn={}",
                categoryId, propertyId, request.getRequiredYn());

        if (!categoryPropertyMapper.exists(categoryId, propertyId)) {
            throw BusinessException.notFound("카테고리에 해당 속성이 없습니다");
        }

        categoryPropertyMapper.updateRequiredYn(categoryId, propertyId, request.getRequiredYn());
        log.info("Property required status updated");
    }

    // =============================================
    // 권한 확인
    // =============================================

    @Override
    public boolean isOwner(Long categoryId, String username) {
        return categoryMapper.findById(categoryId)
                .map(c -> username.equals(c.getOwnerUsername()))
                .orElse(false);
    }

    @Override
    public boolean hasAccess(Long categoryId, String username, String deptCode) {
        // 소유자인 경우
        if (isOwner(categoryId, username)) {
            return true;
        }

        // 사용자에게 직접 공유된 경우
        if (categoryShareMapper.exists(categoryId, "USER", username)) {
            return true;
        }

        // 부서에 공유된 경우
        if (deptCode != null && categoryShareMapper.exists(categoryId, "DEPARTMENT", deptCode)) {
            return true;
        }

        return false;
    }

    @Override
    public boolean hasEditPermission(Long categoryId, String username, String deptCode) {
        // 소유자인 경우
        if (isOwner(categoryId, username)) {
            return true;
        }

        // 사용자에게 EDIT 권한으로 공유된 경우
        CategoryShare userShare = categoryShareMapper.findByTarget(categoryId, "USER", username)
                .orElse(null);
        if (userShare != null && "EDIT".equals(userShare.getPermission())) {
            return true;
        }

        // 부서에 EDIT 권한으로 공유된 경우
        if (deptCode != null) {
            CategoryShare deptShare = categoryShareMapper.findByTarget(categoryId, "DEPARTMENT", deptCode)
                    .orElse(null);
            if (deptShare != null && "EDIT".equals(deptShare.getPermission())) {
                return true;
            }
        }

        return false;
    }

    // =============================================
    // v2.0: 카테고리 이관 (Transfer)
    // =============================================

    @Override
    @Transactional
    public CategoryResponse transferCategory(Long categoryId, String newOwnerUsername, String transferredBy) {
        log.info("Transferring category: id={}, newOwner={}, by={}",
                categoryId, newOwnerUsername, transferredBy);

        // 1. 카테고리 존재 확인
        Category category = categoryMapper.findById(categoryId)
                .orElseThrow(() -> BusinessException.notFound("카테고리를 찾을 수 없습니다: " + categoryId));

        // 2. 이관 대상 사용자 존재 확인
        User newOwner = userMapper.findByUsername(newOwnerUsername)
                .orElseThrow(() -> BusinessException.userNotFound(newOwnerUsername));

        // 3. 이미 대상 사용자 소유인 경우
        if (newOwnerUsername.equals(category.getOwnerUsername())) {
            throw BusinessException.badRequest("이미 해당 사용자가 소유한 카테고리입니다.");
        }

        // 4. 대상 사용자에게 동일 이름 카테고리가 있는지 확인
        List<Category> targetUserCategories = categoryMapper.findByOwner(newOwnerUsername);
        boolean nameExists = targetUserCategories.stream()
                .anyMatch(c -> c.getCategoryName().equals(category.getCategoryName()));
        if (nameExists) {
            throw BusinessException.conflict(
                    "대상 사용자에게 동일한 이름의 카테고리가 이미 존재합니다: " + category.getCategoryName());
        }

        // 5. 소유권 이전
        String previousOwner = category.getOwnerUsername();
        category.setOwnerUsername(newOwnerUsername);
        category.setUpdatedBy(transferredBy);

        categoryMapper.update(category);
        log.info("Category transferred: id={}, from={}, to={}",
                categoryId, previousOwner, newOwnerUsername);

        return getCategory(categoryId);
    }

    @Override
    @Transactional
    public CategoryResponse copyCategoryToUser(Long categoryId, String newOwnerUsername, String copiedBy) {
        log.info("Copying category to user: id={}, newOwner={}, by={}",
                categoryId, newOwnerUsername, copiedBy);

        // 1. 원본 카테고리 조회 (상세 포함)
        Category originalCategory = categoryMapper.findByIdWithDetails(categoryId)
                .orElseThrow(() -> BusinessException.notFound("카테고리를 찾을 수 없습니다: " + categoryId));

        // 2. 대상 사용자 존재 확인
        User newOwner = userMapper.findByUsername(newOwnerUsername)
                .orElseThrow(() -> BusinessException.userNotFound(newOwnerUsername));

        // 3. 대상 사용자에게 동일 이름 카테고리가 있는지 확인
        List<Category> targetUserCategories = categoryMapper.findByOwner(newOwnerUsername);
        final String baseName = originalCategory.getCategoryName();

        // 이름 충돌 여부 확인 및 고유 이름 생성
        Set<String> existingNames = targetUserCategories.stream()
                .map(Category::getCategoryName)
                .collect(Collectors.toSet());

        String newCategoryName = baseName;
        if (existingNames.contains(baseName)) {
            int suffix = 1;
            while (existingNames.contains(baseName + "_copy" + suffix)) {
                suffix++;
            }
            newCategoryName = baseName + "_copy" + suffix;
        }

        // 4. 새 카테고리 코드 생성
        String newCategoryCode = "CAT_" + System.currentTimeMillis();

        // 5. 정렬 순서 설정
        Integer sortOrder = categoryMapper.getMaxSortOrderByOwner(newOwnerUsername) + 1;

        // 6. 새 카테고리 생성
        Category newCategory = Category.builder()
                .categoryCode(newCategoryCode)
                .categoryName(newCategoryName)
                .description(originalCategory.getDescription())
                .categoryColor(originalCategory.getCategoryColor())
                .ownerUsername(newOwnerUsername)
                .sortOrder(sortOrder)
                .useYn("Y")
                .createdBy(copiedBy)
                .build();

        categoryMapper.insert(newCategory);
        log.info("Category copied: originalId={}, newId={}, newOwner={}",
                categoryId, newCategory.getCategoryId(), newOwnerUsername);

        // 7. 속성 연결 복사
        List<CategoryProperty> originalProperties = categoryPropertyMapper.findByCategoryId(categoryId);
        if (!originalProperties.isEmpty()) {
            List<CategoryProperty> newProperties = new ArrayList<>();
            for (CategoryProperty origProp : originalProperties) {
                newProperties.add(CategoryProperty.builder()
                        .categoryId(newCategory.getCategoryId())
                        .propertyId(origProp.getPropertyId())
                        .sortOrder(origProp.getSortOrder())
                        .defaultValue(origProp.getDefaultValue())
                        .requiredYn(origProp.getRequiredYn())
                        .createdBy(copiedBy)
                        .build());
            }
            categoryPropertyMapper.insertBatch(newProperties);
            log.info("Category property mappings copied: count={}", newProperties.size());
        }

        return getCategory(newCategory.getCategoryId());
    }

    @Override
    @Transactional
    public List<CategoryResponse> transferCategories(List<Long> categoryIds, String newOwnerUsername, String transferredBy) {
        log.info("Batch transferring categories: count={}, newOwner={}",
                categoryIds.size(), newOwnerUsername);

        List<CategoryResponse> results = new ArrayList<>();

        for (Long categoryId : categoryIds) {
            try {
                CategoryResponse transferred = transferCategory(categoryId, newOwnerUsername, transferredBy);
                results.add(transferred);
            } catch (BusinessException e) {
                log.warn("Failed to transfer category {}: {}", categoryId, e.getMessage());
                // 개별 실패는 건너뛰고 계속 진행
            }
        }

        log.info("Batch transfer completed: success={}/{}", results.size(), categoryIds.size());
        return results;
    }
}
