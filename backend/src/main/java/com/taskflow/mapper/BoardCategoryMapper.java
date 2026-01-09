package com.taskflow.mapper;

import com.taskflow.domain.BoardCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 보드-카테고리 연결 Mapper 인터페이스
 */
@Mapper
public interface BoardCategoryMapper {

    // =============================================
    // 조회
    // =============================================

    /**
     * ID로 조회
     */
    Optional<BoardCategory> findById(@Param("id") Long id);

    /**
     * 보드별 카테고리 목록 조회
     */
    List<BoardCategory> findByBoardId(@Param("boardId") Long boardId);

    /**
     * 카테고리별 보드 목록 조회
     */
    List<BoardCategory> findByCategoryId(@Param("categoryId") Long categoryId);

    /**
     * 특정 보드-카테고리 연결 조회
     */
    Optional<BoardCategory> findByIds(@Param("boardId") Long boardId, @Param("categoryId") Long categoryId);

    /**
     * 연결 존재 여부 확인
     */
    boolean exists(@Param("boardId") Long boardId, @Param("categoryId") Long categoryId);

    /**
     * 보드의 최대 정렬 순서 조회
     */
    Integer getMaxSortOrder(@Param("boardId") Long boardId);

    /**
     * 보드의 기본 카테고리 조회 (v2.0)
     */
    Optional<BoardCategory> findDefaultByBoardId(@Param("boardId") Long boardId);

    // =============================================
    // 등록/수정/삭제
    // =============================================

    /**
     * 카테고리 연결 등록
     */
    int insert(BoardCategory boardCategory);

    /**
     * 일괄 카테고리 연결 등록
     */
    int insertBatch(@Param("list") List<BoardCategory> list);

    /**
     * 정렬 순서 수정
     */
    int updateSortOrder(@Param("boardId") Long boardId,
                        @Param("categoryId") Long categoryId,
                        @Param("sortOrder") Integer sortOrder);

    /**
     * 기본 카테고리 설정 (v2.0)
     */
    int updateDefaultYn(@Param("boardId") Long boardId,
                        @Param("categoryId") Long categoryId,
                        @Param("defaultYn") String defaultYn);

    /**
     * 보드의 기본 카테고리 해제 (v2.0)
     */
    int clearDefaultCategory(@Param("boardId") Long boardId);

    /**
     * 카테고리 연결 삭제
     */
    int delete(@Param("boardId") Long boardId, @Param("categoryId") Long categoryId);

    /**
     * 보드의 모든 카테고리 연결 삭제
     */
    int deleteByBoardId(@Param("boardId") Long boardId);
}
