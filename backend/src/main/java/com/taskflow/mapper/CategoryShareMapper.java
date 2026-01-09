package com.taskflow.mapper;

import com.taskflow.domain.CategoryShare;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 카테고리 공유 Mapper 인터페이스
 */
@Mapper
public interface CategoryShareMapper {

    // =============================================
    // 조회
    // =============================================

    /**
     * 공유 ID로 조회
     */
    Optional<CategoryShare> findById(@Param("shareId") Long shareId);

    /**
     * 카테고리별 공유 목록 조회
     */
    List<CategoryShare> findByCategoryId(@Param("categoryId") Long categoryId);

    /**
     * 특정 공유 대상으로 조회
     */
    Optional<CategoryShare> findByTarget(@Param("categoryId") Long categoryId,
                                          @Param("shareType") String shareType,
                                          @Param("shareTarget") String shareTarget);

    /**
     * 공유 존재 여부 확인
     */
    boolean exists(@Param("categoryId") Long categoryId,
                   @Param("shareType") String shareType,
                   @Param("shareTarget") String shareTarget);

    // =============================================
    // 등록/수정/삭제
    // =============================================

    /**
     * 공유 등록
     */
    int insert(CategoryShare share);

    /**
     * 권한 수정
     */
    int updatePermission(@Param("shareId") Long shareId,
                         @Param("permission") String permission,
                         @Param("updatedBy") String updatedBy);

    /**
     * 공유 삭제
     */
    int delete(@Param("shareId") Long shareId);

    /**
     * 카테고리의 모든 공유 삭제
     */
    int deleteByCategoryId(@Param("categoryId") Long categoryId);
}
