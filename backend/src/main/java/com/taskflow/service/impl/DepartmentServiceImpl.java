package com.taskflow.service.impl;

import com.taskflow.domain.Department;
import com.taskflow.dto.department.*;
import com.taskflow.dto.user.UserResponse;
import com.taskflow.exception.BusinessException;
import com.taskflow.mapper.DepartmentMapper;
import com.taskflow.mapper.UserMapper;
import com.taskflow.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 부서 서비스 구현
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentMapper departmentMapper;
    private final UserMapper userMapper;

    // =============================================
    // 조회
    // =============================================

    @Override
    public DepartmentResponse getDepartment(Long departmentId) {
        Department department = departmentMapper.findById(departmentId)
                .orElseThrow(() -> BusinessException.departmentNotFound(departmentId));
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
    public List<DepartmentResponse> getChildDepartments(Long departmentId) {
        // 부서 존재 확인
        departmentMapper.findById(departmentId)
                .orElseThrow(() -> BusinessException.departmentNotFound(departmentId));

        return departmentMapper.findChildren(departmentId).stream()
                .map(DepartmentResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserResponse> getDepartmentUsers(Long departmentId) {
        // 부서 존재 확인
        departmentMapper.findById(departmentId)
                .orElseThrow(() -> BusinessException.departmentNotFound(departmentId));

        return userMapper.findByDepartmentId(departmentId).stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
    }

    // =============================================
    // 등록/수정/삭제
    // =============================================

    @Override
    @Transactional
    public DepartmentResponse createDepartment(DepartmentCreateRequest request, Long createdBy) {
        log.info("Creating department: code={}", request.getDepartmentCode());

        // 부서 코드 중복 확인
        if (departmentMapper.existsByCode(request.getDepartmentCode())) {
            throw BusinessException.duplicateCode(request.getDepartmentCode());
        }

        // 상위 부서 존재 확인
        if (request.getParentId() != null) {
            departmentMapper.findById(request.getParentId())
                    .orElseThrow(() -> BusinessException.departmentNotFound(request.getParentId()));
        }

        // 정렬 순서 자동 설정
        Integer sortOrder = request.getSortOrder();
        if (sortOrder == null) {
            sortOrder = departmentMapper.getMaxSortOrder(request.getParentId()) + 1;
        }

        // 부서 엔티티 생성
        Department department = Department.builder()
                .departmentCode(request.getDepartmentCode())
                .departmentName(request.getDepartmentName())
                .parentId(request.getParentId())
                .sortOrder(sortOrder)
                .useYn("Y")
                .createdBy(createdBy)
                .build();

        // 저장
        departmentMapper.insert(department);
        log.info("Department created: id={}, code={}", department.getDepartmentId(), department.getDepartmentCode());

        return getDepartment(department.getDepartmentId());
    }

    @Override
    @Transactional
    public DepartmentResponse updateDepartment(Long departmentId, DepartmentUpdateRequest request, Long updatedBy) {
        log.info("Updating department: id={}", departmentId);

        // 부서 존재 확인
        Department department = departmentMapper.findById(departmentId)
                .orElseThrow(() -> BusinessException.departmentNotFound(departmentId));

        // 상위 부서 변경 시 순환 참조 검증
        if (request.getParentId() != null && !Objects.equals(department.getParentId(), request.getParentId())) {
            validateNotCircular(departmentId, request.getParentId());
        }

        // 상위 부서 존재 확인
        if (request.getParentId() != null) {
            departmentMapper.findById(request.getParentId())
                    .orElseThrow(() -> BusinessException.departmentNotFound(request.getParentId()));
        }

        // 수정
        department.setDepartmentName(request.getDepartmentName());
        department.setParentId(request.getParentId());
        if (request.getSortOrder() != null) {
            department.setSortOrder(request.getSortOrder());
        }
        if (request.getUseYn() != null) {
            department.setUseYn(request.getUseYn());
        }
        department.setUpdatedBy(updatedBy);

        departmentMapper.update(department);
        log.info("Department updated: id={}", departmentId);

        return getDepartment(departmentId);
    }

    @Override
    @Transactional
    public DepartmentResponse updateDepartmentOrder(Long departmentId, DepartmentOrderRequest request, Long updatedBy) {
        log.info("Updating department order: id={}, newOrder={}", departmentId, request.getSortOrder());

        // 부서 존재 확인
        Department department = departmentMapper.findById(departmentId)
                .orElseThrow(() -> BusinessException.departmentNotFound(departmentId));

        // 상위 부서 변경 시 순환 참조 검증
        if (request.getParentId() != null && !Objects.equals(department.getParentId(), request.getParentId())) {
            validateNotCircular(departmentId, request.getParentId());
        }

        // 순서 변경
        departmentMapper.updateOrder(departmentId, request.getParentId(), request.getSortOrder(), updatedBy);
        log.info("Department order updated: id={}", departmentId);

        return getDepartment(departmentId);
    }

    @Override
    @Transactional
    public void deleteDepartment(Long departmentId) {
        log.info("Deleting department: id={}", departmentId);

        // 부서 존재 확인
        departmentMapper.findById(departmentId)
                .orElseThrow(() -> BusinessException.departmentNotFound(departmentId));

        // 하위 부서 존재 확인
        if (departmentMapper.hasChildren(departmentId)) {
            throw BusinessException.dataInUse("하위 부서가 존재하여 삭제할 수 없습니다. 하위 부서를 먼저 삭제해주세요.");
        }

        // 소속 사용자 존재 확인
        if (departmentMapper.hasUsers(departmentId)) {
            throw BusinessException.dataInUse("소속된 사용자가 존재하여 삭제할 수 없습니다. 사용자를 다른 부서로 이동해주세요.");
        }

        // 삭제
        departmentMapper.delete(departmentId);
        log.info("Department deleted: id={}", departmentId);
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

        // ID로 인덱싱
        Map<Long, Department> departmentMap = new LinkedHashMap<>();
        for (Department dept : flatList) {
            dept.setChildren(new ArrayList<>());
            departmentMap.put(dept.getDepartmentId(), dept);
        }

        // 트리 구조 구성
        List<Department> roots = new ArrayList<>();
        for (Department dept : flatList) {
            if (dept.getParentId() == null) {
                roots.add(dept);
            } else {
                Department parent = departmentMap.get(dept.getParentId());
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
    private void validateNotCircular(Long departmentId, Long newParentId) {
        if (departmentId.equals(newParentId)) {
            throw BusinessException.badRequest("자기 자신을 상위 부서로 지정할 수 없습니다");
        }

        // 새 상위 부서의 모든 상위 부서를 조회하여 순환 참조 확인
        List<Department> ancestors = departmentMapper.findAncestors(newParentId);
        for (Department ancestor : ancestors) {
            if (ancestor.getDepartmentId().equals(departmentId)) {
                throw BusinessException.badRequest("하위 부서를 상위 부서로 지정할 수 없습니다 (순환 참조)");
            }
        }

        // 새 상위 부서가 현재 부서의 하위 부서인지 확인
        List<Department> descendants = departmentMapper.findAllDescendants(departmentId);
        for (Department descendant : descendants) {
            if (descendant.getDepartmentId().equals(newParentId)) {
                throw BusinessException.badRequest("하위 부서를 상위 부서로 지정할 수 없습니다 (순환 참조)");
            }
        }
    }
}
