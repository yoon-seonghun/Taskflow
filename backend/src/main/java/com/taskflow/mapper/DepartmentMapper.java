package com.taskflow.mapper;

import com.taskflow.domain.Department;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 부서 Mapper 인터페이스
 */
@Mapper
public interface DepartmentMapper {

    // =============================================
    // 조회
    // =============================================

    /**
     * 부서 ID로 조회
     */
    Optional<Department> findById(@Param("departmentId") Long departmentId);

    /**
     * 부서 코드로 조회
     */
    Optional<Department> findByCode(@Param("departmentCode") String departmentCode);

    /**
     * 전체 부서 목록 조회 (평면 구조, 계층 순서)
     * WITH RECURSIVE를 사용하여 계층 순서대로 정렬
     */
    List<Department> findAllFlat(@Param("useYn") String useYn);

    /**
     * 최상위 부서 목록 조회 (PARENT_ID IS NULL)
     */
    List<Department> findRootDepartments(@Param("useYn") String useYn);

    /**
     * 특정 부서의 하위 부서 목록 조회 (직계 자식만)
     */
    List<Department> findChildren(@Param("parentCode") String parentCode);

    /**
     * 특정 부서의 모든 하위 부서 조회 (재귀)
     */
    List<Department> findAllDescendants(@Param("departmentCode") String departmentCode);

    /**
     * 특정 부서의 상위 부서 경로 조회 (루트까지)
     */
    List<Department> findAncestors(@Param("departmentCode") String departmentCode);

    /**
     * 부서 코드 중복 확인
     */
    boolean existsByCode(@Param("departmentCode") String departmentCode);

    /**
     * 부서 코드 중복 확인 (자신 제외)
     */
    boolean existsByCodeAndIdNot(
            @Param("departmentCode") String departmentCode,
            @Param("departmentId") Long departmentId
    );

    /**
     * 특정 부서에 하위 부서가 있는지 확인
     */
    boolean hasChildren(@Param("departmentCode") String departmentCode);

    /**
     * 특정 부서에 소속된 사용자가 있는지 확인
     */
    boolean hasUsers(@Param("departmentCode") String departmentCode);

    /**
     * 특정 부서의 최대 정렬 순서 조회
     */
    Integer getMaxSortOrder(@Param("parentCode") String parentCode);

    // =============================================
    // 등록/수정/삭제
    // =============================================

    /**
     * 부서 등록
     */
    int insert(Department department);

    /**
     * 부서 수정
     */
    int update(Department department);

    /**
     * 부서 순서 변경 (코드 기준)
     */
    int updateOrder(
            @Param("departmentCode") String departmentCode,
            @Param("parentCode") String parentCode,
            @Param("sortOrder") Integer sortOrder,
            @Param("updatedBy") String updatedBy
    );

    /**
     * 부서 삭제 (물리 삭제) - 코드 기준
     */
    int delete(@Param("departmentCode") String departmentCode);

    /**
     * 부서 비활성화 (논리 삭제) - 코드 기준
     */
    int deactivate(
            @Param("departmentCode") String departmentCode,
            @Param("updatedBy") String updatedBy
    );

    /**
     * 같은 상위 부서 내에서 정렬 순서 재정렬
     * (특정 순서 이상의 부서들의 순서를 +1)
     */
    int incrementSortOrder(
            @Param("parentCode") String parentCode,
            @Param("fromOrder") Integer fromOrder
    );

    // =============================================
    // 부서 동기화 (UPSERT)
    // =============================================

    /**
     * 부서 정보 UPSERT (departmentCode 기준)
     * 존재하면 UPDATE, 없으면 INSERT
     */
    int upsertByCode(Department department);

    /**
     * 지정된 departmentCode 목록에 없는 부서 비활성화
     * (외부 시스템과 동기화 시 사용)
     */
    int deactivateDepartmentsNotIn(@Param("activeCodes") List<String> activeCodes);
}
