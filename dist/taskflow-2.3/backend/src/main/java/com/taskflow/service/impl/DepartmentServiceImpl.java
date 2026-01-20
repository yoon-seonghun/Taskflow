package com.taskflow.service.impl;

import com.taskflow.config.UserManagementProperties;
import com.taskflow.domain.Department;
import com.taskflow.dto.department.*;
import com.taskflow.dto.user.UserResponse;
import com.taskflow.exception.BusinessException;
import com.taskflow.mapper.DepartmentMapper;
import com.taskflow.mapper.UserMapper;
import com.taskflow.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 부서 서비스 구현 (Internal 모드)
 *
 * 기본 관리 모드로 TaskFlow DB에서 부서 데이터를 직접 관리
 * - 부서 CRUD 가능
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@ConditionalOnProperty(
    name = "taskflow.user-management.mode",
    havingValue = "internal",
    matchIfMissing = true
)
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentMapper departmentMapper;
    private final UserMapper userMapper;
    private final UserManagementProperties userManagementProperties;

    // =============================================
    // 모드 확인
    // =============================================

    @Override
    public boolean isCrudEnabled() {
        return userManagementProperties.isDepartmentCrudEnabled();
    }

    // =============================================
    // 조회
    // =============================================

    @Override
    public DepartmentResponse getDepartment(String departmentCode) {
        Department department = departmentMapper.findByCode(departmentCode)
                .orElseThrow(() -> BusinessException.departmentNotFound(departmentCode));
        return DepartmentResponse.from(department);
    }

    @Override
    public List<DepartmentTreeResponse> getDepartmentTree(String useYn) {
        // 평면 목록 조회 (계층 순서)
        List<Department> flatList = departmentMapper.findAllFlat(useYn);

        // 트리 구조로 변환
        List<Department> tree = buildTree(flatList);

        return DepartmentTreeResponse.fromList(tree);
    }

    @Override
    public List<DepartmentFlatResponse> getDepartmentsFlat(String useYn) {
        List<Department> departments = departmentMapper.findAllFlat(useYn);
        return DepartmentFlatResponse.fromList(departments);
    }

    @Override
    public List<DepartmentResponse> getChildDepartments(String departmentCode) {
        // 부서 존재 확인
        departmentMapper.findByCode(departmentCode)
                .orElseThrow(() -> BusinessException.departmentNotFound(departmentCode));

        return departmentMapper.findChildren(departmentCode).stream()
                .map(DepartmentResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserResponse> getDepartmentUsers(String departmentCode) {
        // 부서 존재 확인
        departmentMapper.findByCode(departmentCode)
                .orElseThrow(() -> BusinessException.departmentNotFound(departmentCode));

        return userMapper.findByDepartmentCode(departmentCode).stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
    }

    // =============================================
    // 등록/수정/삭제
    // =============================================

    @Override
    @Transactional
    public DepartmentResponse createDepartment(DepartmentCreateRequest request, String createdBy) {
        log.info("Creating department: code={}", request.getDepartmentCode());

        // 부서 코드 중복 확인
        if (departmentMapper.existsByCode(request.getDepartmentCode())) {
            throw BusinessException.duplicateCode(request.getDepartmentCode());
        }

        // 상위 부서 존재 확인
        if (request.getParentCode() != null) {
            departmentMapper.findByCode(request.getParentCode())
                    .orElseThrow(() -> BusinessException.departmentNotFound(request.getParentCode()));
        }

        // 정렬 순서 자동 설정
        Integer sortOrder = request.getSortOrder();
        if (sortOrder == null) {
            sortOrder = departmentMapper.getMaxSortOrder(request.getParentCode()) + 1;
        }

        // 부서 엔티티 생성
        Department department = Department.builder()
                .departmentCode(request.getDepartmentCode())
                .departmentName(request.getDepartmentName())
                .parentCode(request.getParentCode())
                .sortOrder(sortOrder)
                .useYn("Y")
                .createdBy(createdBy)
                .build();

        // 저장
        departmentMapper.insert(department);
        log.info("Department created: code={}", department.getDepartmentCode());

        return getDepartment(department.getDepartmentCode());
    }

    @Override
    @Transactional
    public DepartmentResponse updateDepartment(String departmentCode, DepartmentUpdateRequest request, String updatedBy) {
        log.info("Updating department: code={}", departmentCode);

        // 부서 존재 확인
        Department department = departmentMapper.findByCode(departmentCode)
                .orElseThrow(() -> BusinessException.departmentNotFound(departmentCode));

        // 상위 부서 변경 시 순환 참조 검증
        if (request.getParentCode() != null && !Objects.equals(department.getParentCode(), request.getParentCode())) {
            validateNotCircular(departmentCode, request.getParentCode());
        }

        // 상위 부서 존재 확인
        if (request.getParentCode() != null) {
            departmentMapper.findByCode(request.getParentCode())
                    .orElseThrow(() -> BusinessException.departmentNotFound(request.getParentCode()));
        }

        // 수정
        department.setDepartmentName(request.getDepartmentName());
        department.setParentCode(request.getParentCode());
        if (request.getSortOrder() != null) {
            department.setSortOrder(request.getSortOrder());
        }
        if (request.getUseYn() != null) {
            department.setUseYn(request.getUseYn());
        }
        department.setUpdatedBy(updatedBy);

        departmentMapper.update(department);
        log.info("Department updated: code={}", departmentCode);

        return getDepartment(departmentCode);
    }

    @Override
    @Transactional
    public DepartmentResponse updateDepartmentOrder(String departmentCode, DepartmentOrderRequest request, String updatedBy) {
        log.info("Updating department order: code={}, newOrder={}", departmentCode, request.getSortOrder());

        // 부서 존재 확인
        Department department = departmentMapper.findByCode(departmentCode)
                .orElseThrow(() -> BusinessException.departmentNotFound(departmentCode));

        // 상위 부서 변경 시 순환 참조 검증
        if (request.getParentCode() != null && !Objects.equals(department.getParentCode(), request.getParentCode())) {
            validateNotCircular(departmentCode, request.getParentCode());
        }

        // 순서 변경
        departmentMapper.updateOrder(departmentCode, request.getParentCode(), request.getSortOrder(), updatedBy);
        log.info("Department order updated: code={}", departmentCode);

        return getDepartment(departmentCode);
    }

    @Override
    @Transactional
    public void deleteDepartment(String departmentCode) {
        log.info("Deleting department: code={}", departmentCode);

        // 부서 존재 확인
        departmentMapper.findByCode(departmentCode)
                .orElseThrow(() -> BusinessException.departmentNotFound(departmentCode));

        // 하위 부서 존재 확인
        if (departmentMapper.hasChildren(departmentCode)) {
            throw BusinessException.dataInUse("하위 부서가 존재하여 삭제할 수 없습니다. 하위 부서를 먼저 삭제해주세요.");
        }

        // 소속 사용자 존재 확인
        if (departmentMapper.hasUsers(departmentCode)) {
            throw BusinessException.dataInUse("소속된 사용자가 존재하여 삭제할 수 없습니다. 사용자를 다른 부서로 이동해주세요.");
        }

        // 삭제
        departmentMapper.delete(departmentCode);
        log.info("Department deleted: code={}", departmentCode);
    }

    // =============================================
    // 검증
    // =============================================

    @Override
    public boolean existsByCode(String departmentCode) {
        return departmentMapper.existsByCode(departmentCode);
    }

    // =============================================
    // Private Methods
    // =============================================

    /**
     * 평면 목록을 트리 구조로 변환
     */
    private List<Department> buildTree(List<Department> flatList) {
        if (flatList == null || flatList.isEmpty()) {
            return new ArrayList<>();
        }

        // 코드로 인덱싱
        Map<String, Department> departmentMap = new LinkedHashMap<>();
        for (Department dept : flatList) {
            dept.setChildren(new ArrayList<>());
            departmentMap.put(dept.getDepartmentCode(), dept);
        }

        // 트리 구조 구성
        List<Department> roots = new ArrayList<>();
        for (Department dept : flatList) {
            if (dept.getParentCode() == null) {
                roots.add(dept);
            } else {
                Department parent = departmentMap.get(dept.getParentCode());
                if (parent != null) {
                    parent.addChild(dept);
                }
            }
        }

        return roots;
    }

    /**
     * 순환 참조 검증
     * 자신의 하위 부서를 상위 부서로 지정하려는 경우 예외 발생
     */
    private void validateNotCircular(String departmentCode, String newParentCode) {
        if (departmentCode.equals(newParentCode)) {
            throw BusinessException.badRequest("자기 자신을 상위 부서로 지정할 수 없습니다");
        }

        // 새 상위 부서의 모든 상위 부서를 조회하여 순환 참조 확인
        List<Department> ancestors = departmentMapper.findAncestors(newParentCode);
        for (Department ancestor : ancestors) {
            if (ancestor.getDepartmentCode().equals(departmentCode)) {
                throw BusinessException.badRequest("하위 부서를 상위 부서로 지정할 수 없습니다 (순환 참조)");
            }
        }

        // 새 상위 부서가 현재 부서의 하위 부서인지 확인
        List<Department> descendants = departmentMapper.findAllDescendants(departmentCode);
        for (Department descendant : descendants) {
            if (descendant.getDepartmentCode().equals(newParentCode)) {
                throw BusinessException.badRequest("하위 부서를 상위 부서로 지정할 수 없습니다 (순환 참조)");
            }
        }
    }
}
