package com.taskflow.mapper.external;

import com.taskflow.domain.Position;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 외부 DB 직급 Mapper 인터페이스
 *
 * 읽기 전용 - CRUD 불가
 * 외부 DB의 V_TASKFLOW_POSITION VIEW에서 데이터 조회
 */
@Mapper
public interface ExternalPositionMapper {

    /**
     * 직급 ID로 조회
     */
    Optional<Position> findById(@Param("positionId") Long positionId);

    /**
     * 직급 코드로 조회
     */
    Optional<Position> findByCode(@Param("positionCode") String positionCode);

    /**
     * 전체 직급 목록 조회
     *
     * @param useYn 사용 여부 필터 (null이면 전체)
     * @return 직급 목록 (SORT_ORDER 순)
     */
    List<Position> findAll(@Param("useYn") String useYn);

    /**
     * 직급 코드 존재 확인
     */
    boolean existsByCode(@Param("positionCode") String positionCode);

    /**
     * 전체 활성 직급 목록 조회 (동기화용)
     */
    List<Position> findAllActive();
}
