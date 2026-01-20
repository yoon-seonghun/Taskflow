package com.taskflow.service;

import com.taskflow.dto.department.*;
import com.taskflow.dto.user.UserResponse;

import java.util.List;

/**
 * 부서 서비스 인터페이스
 *
 * 두 가지 모드 지원:
 * - internal: 기본 관리 모드 (CRUD 가능)
 * - external: 외부 DB 연동 모드 (조회만 가능)
 */
public interface DepartmentService {

    // =============================================
    // 모드 확인
    // =============================================

    /**
     * CRUD 활성화 여부 확인
     * - internal 모드: true (기본)
     * - external 모드: false
     *
     * @return CRUD 활성화 여부
     */
    boolean isCrudEnabled();

    // =============================================
    // 조회
    // =============================================

    /**
     * 부서 코드로 조회
     *
     * @param departmentCode 부서 코드
     * @return 부서 응답
     */
    DepartmentResponse getDepartment(String departmentCode);

    /**
     * 부서 목록 조회 (트리 구조)
     *
     * @param useYn 사용 여부 필터 (null = 전체)
     * @return 트리 구조 부서 목록
     */
    List<DepartmentTreeResponse> getDepartmentTree(String useYn);

    /**
     * 부서 목록 조회 (평면 구조)
     * SELECT 박스용 - 계층 순서대로 정렬
     *
     * @param useYn 사용 여부 필터 (null = 전체)
     * @return 평면 구조 부서 목록
     */
    List<DepartmentFlatResponse> getDepartmentsFlat(String useYn);

    /**
     * 특정 부서의 하위 부서 목록 조회
     *
     * @param departmentCode 부서 코드
     * @return 하위 부서 목록
     */
    List<DepartmentResponse> getChildDepartments(String departmentCode);

    /**
     * 부서별 사용자 목록 조회
     *
     * @param departmentCode 부서 코드
     * @return 사용자 목록
     */
    List<UserResponse> getDepartmentUsers(String departmentCode);

    // =============================================
    // 등록/수정/삭제
    // =============================================

    /**
     * 부서 등록
     *
     * @param request 등록 요청
     * @param createdBy 생성자 Username
     * @return 생성된 부서 응답
     */
    DepartmentResponse createDepartment(DepartmentCreateRequest request, String createdBy);

    /**
     * 부서 수정
     *
     * @param departmentCode 부서 코드
     * @param request 수정 요청
     * @param updatedBy 수정자 Username
     * @return 수정된 부서 응답
     */
    DepartmentResponse updateDepartment(String departmentCode, DepartmentUpdateRequest request, String updatedBy);

    /**
     * 부서 순서 변경
     *
     * @param departmentCode 부서 코드
     * @param request 순서 변경 요청
     * @param updatedBy 수정자 Username
     * @return 수정된 부서 응답
     */
    DepartmentResponse updateDepartmentOrder(String departmentCode, DepartmentOrderRequest request, String updatedBy);

    /**
     * 부서 삭제
     *
     * @param departmentCode 부서 코드
     */
    void deleteDepartment(String departmentCode);

    // =============================================
    // 검증
    // =============================================

    /**
     * 부서 코드 중복 확인
     *
     * @param departmentCode 부서 코드
     * @return 중복 여부
     */
    boolean existsByCode(String departmentCode);
}
