package com.taskflow.mapper;

import com.taskflow.domain.Position;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 직급 Mapper 인터페이스
 */
@Mapper
public interface PositionMapper {

    // =============================================
    // 조회
    // =============================================

    /**
     * 직급 ID로 조회
     */
    Optional<Position> findById(@Param("positionId") Long positionId);

    /**
     * 직급 코드로 조회
     */
    Optional<Position> findByCode(@Param("positionCode") String positionCode);

    /**
     * 전체 직급 목록 조회 (정렬 순서대로)
     */
    List<Position> findAll(@Param("useYn") String useYn);

    /**
     * 직급 코드 중복 확인
     */
    boolean existsByCode(@Param("positionCode") String positionCode);

    /**
     * 직급 코드 중복 확인 (자신 제외)
     */
    boolean existsByCodeAndIdNot(
            @Param("positionCode") String positionCode,
            @Param("positionId") Long positionId
    );

    /**
     * 특정 직급을 사용 중인 사용자 수 조회
     */
    int countUsersByPositionCode(@Param("positionCode") String positionCode);

    /**
     * 최대 정렬 순서 조회
     */
    Integer getMaxSortOrder();

    // =============================================
    // 등록/수정/삭제
    // =============================================

    /**
     * 직급 등록
     */
    int insert(Position position);

    /**
     * 직급 수정
     */
    int update(Position position);

    /**
     * 직급 순서 변경
     */
    int updateSortOrder(
            @Param("positionCode") String positionCode,
            @Param("sortOrder") Integer sortOrder,
            @Param("updatedBy") String updatedBy
    );

    /**
     * 특정 순서 이상의 직급 정렬 순서를 1씩 증가
     * (신규 직급 중간 삽입 시 기존 직급 순서 밀기용)
     */
    int incrementSortOrdersFrom(
            @Param("fromSortOrder") Integer fromSortOrder,
            @Param("updatedBy") String updatedBy
    );

    /**
     * 특정 순서가 이미 존재하는지 확인
     */
    boolean existsBySortOrder(@Param("sortOrder") Integer sortOrder);

    /**
     * 직급 삭제 (물리 삭제)
     */
    int delete(@Param("positionCode") String positionCode);

    /**
     * 직급 비활성화 (논리 삭제)
     */
    int deactivate(
            @Param("positionCode") String positionCode,
            @Param("updatedBy") String updatedBy
    );

    // =============================================
    // 동기화 (External 모드)
    // =============================================

    /**
     * 직급 UPSERT (positionCode 기준)
     */
    int upsertByCode(Position position);

    /**
     * 지정된 positionCode 목록에 없는 직급 비활성화
     */
    int deactivatePositionsNotIn(@Param("activeCodes") List<String> activeCodes);

    /**
     * 모든 직급 코드 목록 조회
     */
    List<String> findAllPositionCodes();
}
