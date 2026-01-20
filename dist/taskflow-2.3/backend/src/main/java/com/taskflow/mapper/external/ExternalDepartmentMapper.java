package com.taskflow.mapper.external;

import com.taskflow.domain.Department;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 외부 DB 부서 Mapper 인터페이스
 *
 * 읽기 전용 - CRUD 불가
 * 외부 DB의 V_TASKFLOW_DEPARTMENT VIEW에서 데이터 조회
 */
@Mapper
public interface ExternalDepartmentMapper {

    /**
     * 부서 ID로 조회
     */
    Optional<Department> findById(@Param("departmentId") Long departmentId);

    /**
     * 부서 코드로 조회
     */
    Optional<Department> findByCode(@Param("departmentCode") String departmentCode);

    /**
     * 전체 부서 목록 조회 (평면 구조)
     */
    List<Department> findAllFlat(@Param("useYn") String useYn);

    /**
     * 최상위 부서 목록 조회 (PARENT_ID IS NULL)
     */
    List<Department> findRootDepartments(@Param("useYn") String useYn);

    /**
     * 특정 부서의 하위 부서 목록 조회 (직계 자식만 - parentId 기반)
     */
    List<Department> findChildren(@Param("parentId") Long parentId);

    /**
     * 특정 부서의 하위 부서 목록 조회 (직계 자식만 - parentCode 기반)
     */
    List<Department> findByParentCode(@Param("parentCode") String parentCode);

    /**
     * 부서 코드 중복 확인
     */
    boolean existsByCode(@Param("departmentCode") String departmentCode);
}
