package com.taskflow.service;

import com.taskflow.dto.category.*;

import java.util.List;

/**
 * 카테고리 서비스 인터페이스
 *
 * v2.0 변경사항:
 * - 소유자 기반 카테고리 관리
 * - 카테고리 공유 기능
 * - 카테고리-속성 매핑 관리
 */
public interface CategoryService {

    // =============================================
    // 카테고리 조회
    // =============================================

    /**
     * 사용 가능한 카테고리 목록 조회 (소유 + 공유받은)
     */
    List<CategoryResponse> getAccessibleCategories(String username, String deptCode);

    /**
     * 내가 소유한 카테고리 목록 조회
     */
    List<CategoryResponse> getMyCateogries(String ownerUsername);

    /**
     * 공유받은 카테고리 목록 조회
     */
    List<CategoryResponse> getSharedWithMeCategories(String username, String deptCode);

    /**
     * 전체 카테고리 목록 조회 (활성만)
     */
    List<CategoryResponse> getCategories();

    /**
     * 전체 카테고리 목록 조회 (삭제 포함)
     */
    List<CategoryResponse> getCategoriesIncludingDeleted(String useYn);

    /**
     * 카테고리 단건 조회
     */
    CategoryResponse getCategory(Long categoryId);

    /**
     * 카테고리 상세 조회 (속성/공유 목록 포함)
     */
    CategoryResponse getCategoryWithDetails(Long categoryId);

    // =============================================
    // 카테고리 생성/수정/삭제
    // =============================================

    /**
     * 카테고리 생성
     */
    CategoryResponse createCategory(CategoryCreateRequest request, String createdBy);

    /**
     * 카테고리 수정
     */
    CategoryResponse updateCategory(Long categoryId, CategoryUpdateRequest request, String updatedBy);

    /**
     * 카테고리 삭제 (논리 삭제)
     */
    void deleteCategory(Long categoryId, String deletedBy);

    // =============================================
    // 카테고리 공유
    // =============================================

    /**
     * 카테고리 공유 추가
     */
    void addShare(Long categoryId, CategoryShareRequest request, String createdBy);

    /**
     * 카테고리 공유 일괄 추가
     */
    void addShares(Long categoryId, CategoryShareRequest.BulkShareRequest request, String createdBy);

    /**
     * 카테고리 공유 삭제
     */
    void removeShare(Long categoryId, Long shareId);

    /**
     * 카테고리 공유 권한 변경
     */
    void updateSharePermission(Long shareId, String permission, String updatedBy);

    // =============================================
    // 카테고리 속성 관리
    // =============================================

    /**
     * 카테고리별 속성 목록 조회
     */
    List<CategoryPropertyResponse> getCategoryProperties(Long categoryId);

    /**
     * 카테고리에 속성 추가
     */
    void addProperty(Long categoryId, CategoryPropertyRequest request, String createdBy);

    /**
     * 카테고리에 속성 일괄 추가
     */
    void addProperties(Long categoryId, CategoryPropertyRequest.BulkAddRequest request, String createdBy);

    /**
     * 카테고리에서 속성 제거
     */
    void removeProperty(Long categoryId, Long propertyId);

    /**
     * 카테고리 속성 순서 변경
     */
    void updatePropertyOrder(Long categoryId, CategoryPropertyRequest.OrderUpdateRequest request);

    /**
     * 카테고리 속성 기본값 수정
     */
    void updatePropertyDefaultValue(Long categoryId, Long propertyId,
                                    CategoryPropertyRequest.DefaultValueUpdateRequest request);

    /**
     * 카테고리 속성 필수 여부 수정 (v2.0)
     */
    void updatePropertyRequiredYn(Long categoryId, Long propertyId,
                                  CategoryPropertyRequest.RequiredYnUpdateRequest request);

    // =============================================
    // 권한 확인
    // =============================================

    /**
     * 카테고리 소유자 여부 확인
     */
    boolean isOwner(Long categoryId, String username);

    /**
     * 카테고리 접근 권한 확인 (소유 또는 공유)
     */
    boolean hasAccess(Long categoryId, String username, String deptCode);

    /**
     * 카테고리 편집 권한 확인 (소유 또는 EDIT 공유)
     */
    boolean hasEditPermission(Long categoryId, String username, String deptCode);

    // =============================================
    // v2.0: 카테고리 이관 (Transfer)
    // =============================================

    /**
     * 카테고리 소유권 이전
     * - 카테고리의 소유자를 다른 사용자로 변경
     * - 카테고리에 연결된 속성은 유지됨
     * - 기존 소유자는 접근 불가 (별도 공유 필요)
     *
     * @param categoryId      카테고리 ID
     * @param newOwnerUsername 새 소유자 USERNAME
     * @param transferredBy   이전 수행자 USERNAME
     * @return 이전된 카테고리 응답
     */
    CategoryResponse transferCategory(Long categoryId, String newOwnerUsername, String transferredBy);

    /**
     * 카테고리 복사 (다른 사용자에게)
     * - 카테고리 정의와 속성 연결을 복사하여 새 소유자에게 제공
     * - 원본 카테고리는 유지됨
     * - 속성 자체는 복사하지 않음 (기존 속성 ID 참조 유지)
     *
     * @param categoryId      원본 카테고리 ID
     * @param newOwnerUsername 새 소유자 USERNAME
     * @param copiedBy        복사 수행자 USERNAME
     * @return 복사된 새 카테고리 응답
     */
    CategoryResponse copyCategoryToUser(Long categoryId, String newOwnerUsername, String copiedBy);

    /**
     * 카테고리 일괄 이전
     * - 여러 카테고리를 한 번에 다른 사용자에게 이전
     *
     * @param categoryIds      카테고리 ID 목록
     * @param newOwnerUsername 새 소유자 USERNAME
     * @param transferredBy    이전 수행자 USERNAME
     * @return 이전된 카테고리 목록
     */
    List<CategoryResponse> transferCategories(List<Long> categoryIds, String newOwnerUsername, String transferredBy);
}
