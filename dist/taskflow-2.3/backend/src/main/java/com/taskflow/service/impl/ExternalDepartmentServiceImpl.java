package com.taskflow.service.impl;

import com.taskflow.config.UserManagementProperties;
import com.taskflow.domain.Department;
import com.taskflow.dto.department.*;
import com.taskflow.dto.user.UserResponse;
import com.taskflow.exception.BusinessException;
import com.taskflow.mapper.external.ExternalDepartmentMapper;
import com.taskflow.mapper.external.ExternalUserMapper;
import com.taskflow.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 부서 서비스 구현 (External 모드)
 *
 * 외부 DB 연동 모드로 읽기 전용
 * - 부서 조회만 가능
 * - CRUD 불가 (외부 시스템에서 관리)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@ConditionalOnProperty(
    name = "taskflow.user-management.mode",
    havingValue = "external"
)
public class ExternalDepartmentServiceImpl implements DepartmentService {

    private final ExternalDepartmentMapper externalDepartmentMapper;
    private final ExternalUserMapper externalUserMapper;
    private final UserManagementProperties userManagementProperties;

    // =============================================
    // 모드 확인
    // =============================================

    @Override
    public boolean isCrudEnabled() {
        return false; // 외부 모드는 항상 읽기 전용
    }

    // =============================================
    // 조회
    // =============================================

    @Override
    public DepartmentResponse getDepartment(String departmentCode) {
        Department department = externalDepartmentMapper.findByCode(departmentCode)
                .orElseThrow(() -> BusinessException.departmentNotFound(departmentCode));
        return DepartmentResponse.from(department);
    }

    @Override
    public List<DepartmentTreeResponse> getDepartmentTree(String useYn) {
        // 평면 목록 조회
        List<Department> flatList = externalDepartmentMapper.findAllFlat(useYn);

        // 트리 구조로 변환
        List<Department> tree = buildTree(flatList);

        return DepartmentTreeResponse.fromList(tree);
    }

    @Override
    public List<DepartmentFlatResponse> getDepartmentsFlat(String useYn) {
        List<Department> departments = externalDepartmentMapper.findAllFlat(useYn);
        return DepartmentFlatResponse.fromList(departments);
    }

    @Override
    public List<DepartmentResponse> getChildDepartments(String departmentCode) {
        // 부서 존재 확인
        externalDepartmentMapper.findByCode(departmentCode)
                .orElseThrow(() -> BusinessException.departmentNotFound(departmentCode));

        return externalDepartmentMapper.findByParentCode(departmentCode).stream()
                .map(DepartmentResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserResponse> getDepartmentUsers(String departmentCode) {
        // 부서 존재 확인
        externalDepartmentMapper.findByCode(departmentCode)
                .orElseThrow(() -> BusinessException.departmentNotFound(departmentCode));

        return externalUserMapper.findByDepartmentCode(departmentCode).stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
    }

    // =============================================
    // 등록/수정/삭제 (읽기 전용으로 모두 예외 발생)
    // =============================================

    @Override
    public DepartmentResponse createDepartment(DepartmentCreateRequest request, String createdBy) {
        throw BusinessException.operationNotAllowed("외부 DB 연동 모드에서는 부서 등록이 불가능합니다.");
    }

    @Override
    public DepartmentResponse updateDepartment(String departmentCode, DepartmentUpdateRequest request, String updatedBy) {
        throw BusinessException.operationNotAllowed("외부 DB 연동 모드에서는 부서 수정이 불가능합니다.");
    }

    @Override
    public DepartmentResponse updateDepartmentOrder(String departmentCode, DepartmentOrderRequest request, String updatedBy) {
        throw BusinessException.operationNotAllowed("외부 DB 연동 모드에서는 부서 순서 변경이 불가능합니다.");
    }

    @Override
    public void deleteDepartment(String departmentCode) {
        throw BusinessException.operationNotAllowed("외부 DB 연동 모드에서는 부서 삭제가 불가능합니다.");
    }

    // =============================================
    // 검증
    // =============================================

    @Override
    public boolean existsByCode(String departmentCode) {
        return externalDepartmentMapper.existsByCode(departmentCode);
    }

    // =============================================
    // Private Methods
    // =============================================

    /**
     * 평면 목록을 트리 구조로 변환 (DEPARTMENT_CODE 기반)
     */
    private List<Department> buildTree(List<Department> flatList) {
        if (flatList == null || flatList.isEmpty()) {
            return new ArrayList<>();
        }

        // CODE로 인덱싱
        Map<String, Department> departmentMap = new LinkedHashMap<>();
        for (Department dept : flatList) {
            dept.setChildren(new ArrayList<>());
            departmentMap.put(dept.getDepartmentCode(), dept);
        }

        // 트리 구조 구성
        List<Department> roots = new ArrayList<>();
        for (Department dept : flatList) {
            if (dept.getParentCode() == null || dept.getParentCode().isEmpty()) {
                roots.add(dept);
            } else {
                Department parent = departmentMap.get(dept.getParentCode());
                if (parent != null) {
                    parent.addChild(dept);
                } else {
                    // 상위 부서를 찾을 수 없으면 루트로 처리
                    roots.add(dept);
                }
            }
        }

        return roots;
    }
}
