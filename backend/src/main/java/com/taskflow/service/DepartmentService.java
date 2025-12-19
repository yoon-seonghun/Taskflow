package com.taskflow.service;

import com.taskflow.dto.department.*;
import com.taskflow.dto.user.UserResponse;

import java.util.List;

/**
 * 부서 서비스 인터페이스
 */
public interface DepartmentService {

    // =============================================
    // 조회
    // =============================================

    /**
     * 부서 ID로 조회
     *
     * @param departmentId 부서 ID
     * @return 부서 응답
     */
    DepartmentResponse getDepartment(Long departmentId);

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
     * @param departmentId 부서 ID
     * @return 하위 부서 목록
     */
    List<DepartmentResponse> getChildDepartments(Long departmentId);

    /**
     * 부서별 사용자 목록 조회
     *
     * @param departmentId 부서 ID
     * @return 사용자 목록
     */
    List<UserResponse> getDepartmentUsers(Long departmentId);

    // =============================================
    // 등록/수정/삭제
    // =============================================

    /**
     * 부서 등록
     *
     * @param request 등록 요청
     * @param createdBy 생성자 ID
     * @return 생성된 부서 응답
     */
    DepartmentResponse createDepartment(DepartmentCreateRequest request, Long createdBy);

    /**
     * 부서 수정
     *
     * @param departmentId 부서 ID
     * @param request 수정 요청
     * @param updatedBy 수정자 ID
     * @return 수정된 부서 응답
     */
    DepartmentResponse updateDepartment(Long departmentId, DepartmentUpdateRequest request, Long updatedBy);

    /**
     * 부서 순서 변경
     *
     * @param departmentId 부서 ID
     * @param request 순서 변경 요청
     * @param updatedBy 수정자 ID
     * @return 수정된 부서 응답
     */
    DepartmentResponse updateDepartmentOrder(Long departmentId, DepartmentOrderRequest request, Long updatedBy);

    /**
     * 부서 삭제
     *
     * @param departmentId 부서 ID
     */
    void deleteDepartment(Long departmentId);

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
