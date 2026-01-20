package com.taskflow.mapper;

import com.taskflow.domain.BoardProperty;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 보드-속성 매핑 Mapper 인터페이스
 *
 * 보드별 속성 설정 (필수여부, 표시여부, 기본값 등)
 */
@Mapper
public interface BoardPropertyMapper {

    // =============================================
    // 조회
    // =============================================

    /**
     * ID로 조회
     */
    Optional<BoardProperty> findById(@Param("boardPropertyId") Long boardPropertyId);

    /**
     * 보드별 속성 설정 목록 조회
     */
    List<BoardProperty> findByBoardId(@Param("boardId") Long boardId);

    /**
     * 속성별 보드 설정 목록 조회
     */
    List<BoardProperty> findByPropertyId(@Param("propertyId") Long propertyId);

    /**
     * 특정 보드-속성 설정 조회
     */
    Optional<BoardProperty> findByIds(@Param("boardId") Long boardId, @Param("propertyId") Long propertyId);

    /**
     * 설정 존재 여부 확인
     */
    boolean exists(@Param("boardId") Long boardId, @Param("propertyId") Long propertyId);

    /**
     * 보드의 최대 정렬 순서 조회
     */
    Integer getMaxSortOrder(@Param("boardId") Long boardId);

    // =============================================
    // 등록/수정/삭제
    // =============================================

    /**
     * 속성 설정 등록
     */
    int insert(BoardProperty boardProperty);

    /**
     * 일괄 속성 설정 등록
     */
    int insertBatch(@Param("list") List<BoardProperty> list);

    /**
     * 속성 설정 수정
     */
    int update(BoardProperty boardProperty);

    /**
     * 정렬 순서 수정
     */
    int updateSortOrder(@Param("boardId") Long boardId,
                        @Param("propertyId") Long propertyId,
                        @Param("sortOrder") Integer sortOrder);

    /**
     * 속성 설정 삭제
     */
    int delete(@Param("boardId") Long boardId, @Param("propertyId") Long propertyId);

    /**
     * 보드의 모든 속성 설정 삭제
     */
    int deleteByBoardId(@Param("boardId") Long boardId);
}
