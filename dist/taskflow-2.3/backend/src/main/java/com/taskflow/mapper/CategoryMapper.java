package com.taskflow.mapper;

import com.taskflow.domain.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 카테고리 Mapper 인터페이스
 *
 * v2.0 변경사항:
 * - 소유자별 조회 메서드 추가
 * - 공유받은 카테고리 조회 메서드 추가
 * - 카테고리 코드 관련 메서드 추가
 */
@Mapper
public interface CategoryMapper {

    // =============================================
    // 조회
    // =============================================

    /**
     * 카테고리 ID로 조회
     */
    Optional<Category> findById(@Param("categoryId") Long categoryId);

    /**
     * 카테고리 ID로 조회 (속성/공유 목록 포함)
     */
    Optional<Category> findByIdWithDetails(@Param("categoryId") Long categoryId);

    /**
     * 카테고리 코드로 조회
     */
    Optional<Category> findByCode(@Param("categoryCode") String categoryCode);

    /**
     * 소유자별 카테고리 목록 조회
     */
    List<Category> findByOwner(@Param("ownerUsername") String ownerUsername);

    /**
     * 공유받은 카테고리 목록 조회 (사용자)
     */
    List<Category> findSharedWithUser(@Param("username") String username);

    /**
     * 공유받은 카테고리 목록 조회 (부서)
     */
    List<Category> findSharedWithDepartment(@Param("deptCode") String deptCode);

    /**
     * 사용 가능한 카테고리 목록 조회 (소유 + 공유받은)
     */
    List<Category> findAccessible(@Param("username") String username, @Param("deptCode") String deptCode);

    /**
     * 전체 카테고리 목록 조회 (활성만)
     */
    List<Category> findAll();

    /**
     * 전체 카테고리 목록 조회 (삭제 포함)
     */
    List<Category> findAllIncludingDeleted(@Param("useYn") String useYn);

    /**
     * 카테고리명으로 조회
     */
    Optional<Category> findByName(@Param("categoryName") String categoryName);

    /**
     * 카테고리명 중복 확인
     */
    boolean existsByName(@Param("categoryName") String categoryName);

    /**
     * 카테고리명 중복 확인 (자신 제외)
     */
    boolean existsByNameAndIdNot(@Param("categoryName") String categoryName,
                                  @Param("categoryId") Long categoryId);

    /**
     * 카테고리 코드 중복 확인
     */
    boolean existsByCode(@Param("categoryCode") String categoryCode);

    /**
     * 카테고리 코드 중복 확인 (자신 제외)
     */
    boolean existsByCodeAndIdNot(@Param("categoryCode") String categoryCode,
                                  @Param("categoryId") Long categoryId);

    /**
     * 최대 정렬 순서 조회
     */
    Integer getMaxSortOrder();

    /**
     * 소유자별 최대 정렬 순서 조회
     */
    Integer getMaxSortOrderByOwner(@Param("ownerUsername") String ownerUsername);

    /**
     * 카테고리 사용 여부 확인 (보드에서 사용 중인지)
     */
    boolean hasBoards(@Param("categoryId") Long categoryId);

    // =============================================
    // 등록/수정/삭제
    // =============================================

    /**
     * 카테고리 등록
     */
    int insert(Category category);

    /**
     * 카테고리 수정
     */
    int update(Category category);

    /**
     * 카테고리 논리 삭제 (USE_YN = 'N')
     */
    int softDelete(@Param("categoryId") Long categoryId, @Param("updatedBy") String updatedBy);

    /**
     * 카테고리 물리 삭제
     */
    int delete(@Param("categoryId") Long categoryId);
}
