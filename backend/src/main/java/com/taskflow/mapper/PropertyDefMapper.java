package com.taskflow.mapper;

import com.taskflow.domain.PropertyDef;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 속성 정의 Mapper 인터페이스
 */
@Mapper
public interface PropertyDefMapper {

    // =============================================
    // 조회
    // =============================================

    /**
     * 속성 정의 ID로 조회
     *
     * @param propertyId 속성 정의 ID
     * @return 속성 정의 (Optional)
     */
    Optional<PropertyDef> findById(@Param("propertyId") Long propertyId);

    /**
     * 보드별 속성 정의 목록 조회
     *
     * @param boardId   보드 ID
     * @param visibleYn 표시 여부 필터 (null = 전체)
     * @return 속성 정의 목록
     */
    List<PropertyDef> findByBoardId(@Param("boardId") Long boardId, @Param("visibleYn") String visibleYn);

    /**
     * 보드별 속성 정의 목록 조회 (옵션 포함)
     *
     * @param boardId   보드 ID
     * @param visibleYn 표시 여부 필터 (null = 전체)
     * @return 속성 정의 목록 (옵션 포함)
     */
    List<PropertyDef> findByBoardIdWithOptions(@Param("boardId") Long boardId, @Param("visibleYn") String visibleYn);

    /**
     * 보드 내 속성명으로 조회
     *
     * @param boardId      보드 ID
     * @param propertyName 속성명
     * @return 속성 정의 (Optional)
     */
    Optional<PropertyDef> findByBoardIdAndName(@Param("boardId") Long boardId, @Param("propertyName") String propertyName);

    /**
     * 보드 내 속성명 중복 확인
     *
     * @param boardId      보드 ID
     * @param propertyName 속성명
     * @return 중복 여부
     */
    boolean existsByBoardIdAndName(@Param("boardId") Long boardId, @Param("propertyName") String propertyName);

    /**
     * 보드 내 속성명 중복 확인 (자신 제외)
     *
     * @param boardId      보드 ID
     * @param propertyName 속성명
     * @param propertyId   제외할 속성 ID
     * @return 중복 여부
     */
    boolean existsByBoardIdAndNameAndIdNot(@Param("boardId") Long boardId,
                                            @Param("propertyName") String propertyName,
                                            @Param("propertyId") Long propertyId);

    /**
     * 보드 내 최대 정렬 순서 조회
     *
     * @param boardId 보드 ID
     * @return 최대 정렬 순서
     */
    Integer getMaxSortOrder(@Param("boardId") Long boardId);

    /**
     * 속성에 값이 사용 중인지 확인
     *
     * @param propertyId 속성 정의 ID
     * @return 사용 여부
     */
    boolean hasPropertyValues(@Param("propertyId") Long propertyId);

    // =============================================
    // 등록/수정/삭제
    // =============================================

    /**
     * 속성 정의 등록
     *
     * @param propertyDef 속성 정의 엔티티
     * @return 영향받은 행 수
     */
    int insert(PropertyDef propertyDef);

    /**
     * 속성 정의 수정
     *
     * @param propertyDef 속성 정의 엔티티
     * @return 영향받은 행 수
     */
    int update(PropertyDef propertyDef);

    /**
     * 속성 정의 삭제
     *
     * @param propertyId 속성 정의 ID
     * @return 영향받은 행 수
     */
    int delete(@Param("propertyId") Long propertyId);

    /**
     * 보드의 모든 속성 정의 삭제
     *
     * @param boardId 보드 ID
     * @return 영향받은 행 수
     */
    int deleteByBoardId(@Param("boardId") Long boardId);

    /**
     * 신규 보드용 기본 속성 정의 일괄 생성
     * (카테고리, 상태, 우선순위, 담당자, 시작일, 마감일)
     *
     * @param boardId   보드 ID
     * @param createdBy 생성자 ID
     * @return 영향받은 행 수
     */
    int insertDefaultProperties(@Param("boardId") Long boardId, @Param("createdBy") Long createdBy);

    /**
     * 보드의 속성 정의 목록 조회 (이름으로 조회용)
     *
     * @param boardId 보드 ID
     * @return 속성 정의 목록
     */
    List<PropertyDef> findAllByBoardId(@Param("boardId") Long boardId);
}
