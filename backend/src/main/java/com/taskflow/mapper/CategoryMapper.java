package com.taskflow.mapper;

import com.taskflow.domain.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 카테고리 Mapper 인터페이스
 */
@Mapper
public interface CategoryMapper {

    // =============================================
    // 조회
    // =============================================

    /**
     * 카테고리 ID로 조회
     *
     * @param categoryId 카테고리 ID
     * @return 카테고리 (Optional)
     */
    Optional<Category> findById(@Param("categoryId") Long categoryId);

    /**
     * 전체 카테고리 목록 조회 (활성만)
     *
     * @return 카테고리 목록
     */
    List<Category> findAll();

    /**
     * 전체 카테고리 목록 조회 (삭제 포함)
     *
     * @param useYn 사용 여부 필터 (null = 전체)
     * @return 카테고리 목록
     */
    List<Category> findAllIncludingDeleted(@Param("useYn") String useYn);

    /**
     * 카테고리명으로 조회
     *
     * @param categoryName 카테고리명
     * @return 카테고리 (Optional)
     */
    Optional<Category> findByName(@Param("categoryName") String categoryName);

    /**
     * 카테고리명 중복 확인
     *
     * @param categoryName 카테고리명
     * @return 중복 여부
     */
    boolean existsByName(@Param("categoryName") String categoryName);

    /**
     * 카테고리명 중복 확인 (자신 제외)
     *
     * @param categoryName 카테고리명
     * @param categoryId   제외할 카테고리 ID
     * @return 중복 여부
     */
    boolean existsByNameAndIdNot(@Param("categoryName") String categoryName,
                                  @Param("categoryId") Long categoryId);

    /**
     * 최대 정렬 순서 조회
     *
     * @return 최대 정렬 순서
     */
    Integer getMaxSortOrder();

    /**
     * 카테고리 사용 여부 확인 (아이템에서 사용 중인지)
     *
     * @param categoryId 카테고리 ID
     * @return 사용 여부
     */
    boolean hasItems(@Param("categoryId") Long categoryId);

    // =============================================
    // 등록/수정/삭제
    // =============================================

    /**
     * 카테고리 등록
     *
     * @param category 카테고리 엔티티
     * @return 영향받은 행 수
     */
    int insert(Category category);

    /**
     * 카테고리 수정
     *
     * @param category 카테고리 엔티티
     * @return 영향받은 행 수
     */
    int update(Category category);

    /**
     * 카테고리 논리 삭제 (USE_YN = 'N')
     *
     * @param categoryId 카테고리 ID
     * @param updatedBy  수정자 USERNAME
     * @return 영향받은 행 수
     */
    int softDelete(@Param("categoryId") Long categoryId, @Param("updatedBy") String updatedBy);

    /**
     * 카테고리 물리 삭제 (사용 금지 - 논리 삭제만 사용 권장)
     *
     * @param categoryId 카테고리 ID
     * @return 영향받은 행 수
     */
    int delete(@Param("categoryId") Long categoryId);
}
