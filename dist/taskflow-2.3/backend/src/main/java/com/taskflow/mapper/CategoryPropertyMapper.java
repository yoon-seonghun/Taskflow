package com.taskflow.mapper;

import com.taskflow.domain.CategoryProperty;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 카테고리-속성 매핑 Mapper 인터페이스
 */
@Mapper
public interface CategoryPropertyMapper {

    // =============================================
    // 조회
    // =============================================

    /**
     * ID로 조회
     */
    Optional<CategoryProperty> findById(@Param("id") Long id);

    /**
     * 카테고리별 속성 목록 조회
     */
    List<CategoryProperty> findByCategoryId(@Param("categoryId") Long categoryId);

    /**
     * 속성별 카테고리 목록 조회
     */
    List<CategoryProperty> findByPropertyId(@Param("propertyId") Long propertyId);

    /**
     * 특정 카테고리-속성 매핑 조회
     */
    Optional<CategoryProperty> findByIds(@Param("categoryId") Long categoryId,
                                          @Param("propertyId") Long propertyId);

    /**
     * 매핑 존재 여부 확인
     */
    boolean exists(@Param("categoryId") Long categoryId, @Param("propertyId") Long propertyId);

    /**
     * 카테고리의 최대 정렬 순서 조회
     */
    Integer getMaxSortOrder(@Param("categoryId") Long categoryId);

    // =============================================
    // 등록/수정/삭제
    // =============================================

    /**
     * 속성 추가
     */
    int insert(CategoryProperty categoryProperty);

    /**
     * 일괄 속성 추가
     */
    int insertBatch(@Param("list") List<CategoryProperty> list);

    /**
     * 정렬 순서 수정
     */
    int updateSortOrder(@Param("categoryId") Long categoryId,
                        @Param("propertyId") Long propertyId,
                        @Param("sortOrder") Integer sortOrder);

    /**
     * 기본값 수정
     */
    int updateDefaultValue(@Param("categoryId") Long categoryId,
                           @Param("propertyId") Long propertyId,
                           @Param("defaultValue") String defaultValue);

    /**
     * 필수 여부 수정 (v2.0)
     */
    int updateRequiredYn(@Param("categoryId") Long categoryId,
                         @Param("propertyId") Long propertyId,
                         @Param("requiredYn") String requiredYn);

    /**
     * 속성 설정 전체 수정 (v2.0)
     */
    int update(CategoryProperty categoryProperty);

    /**
     * 속성 제거
     */
    int delete(@Param("categoryId") Long categoryId, @Param("propertyId") Long propertyId);

    /**
     * 카테고리의 모든 속성 제거
     */
    int deleteByCategoryId(@Param("categoryId") Long categoryId);
}
