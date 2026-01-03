package com.taskflow.service.impl;

import com.taskflow.domain.Department;
import com.taskflow.dto.sync.SyncResult;
import com.taskflow.mapper.DepartmentMapper;
import com.taskflow.mapper.external.ExternalDepartmentMapper;
import com.taskflow.service.DepartmentSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 부서 동기화 서비스 구현
 *
 * External 모드에서만 활성화
 *
 * USERNAME/DEPARTMENT_CODE 기반 FK 참조 시스템:
 * - DEPARTMENT_CODE 기준 UPSERT 방식
 * - 외부/내부 ID 매핑 불필요 (DEPARTMENT_CODE가 자연키)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "taskflow.user-management.mode",
    havingValue = "external"
)
public class DepartmentSyncServiceImpl implements DepartmentSyncService {

    private final DepartmentMapper departmentMapper;
    private final ExternalDepartmentMapper externalDepartmentMapper;

    @Override
    @Transactional
    public boolean syncDepartmentByCode(String departmentCode) {
        if (departmentCode == null || departmentCode.isBlank()) {
            return false;
        }

        log.debug("Syncing department by code: {}", departmentCode);

        // 1. 외부 DB에서 부서 정보 조회
        Optional<Department> externalDeptOpt = externalDepartmentMapper.findByCode(departmentCode);
        if (externalDeptOpt.isEmpty()) {
            log.warn("External department not found: code={}", departmentCode);
            return false;
        }

        Department externalDept = externalDeptOpt.get();

        // 2. 상위 부서가 있으면 먼저 동기화 (재귀)
        if (externalDept.getParentCode() != null) {
            syncDepartmentByCode(externalDept.getParentCode());
        }

        // 3. UPSERT (DEPARTMENT_CODE 기준)
        Department department = Department.builder()
                .departmentCode(externalDept.getDepartmentCode())
                .departmentName(externalDept.getDepartmentName())
                .parentCode(externalDept.getParentCode())
                .sortOrder(externalDept.getSortOrder() != null ? externalDept.getSortOrder() : 0)
                .useYn(externalDept.getUseYn() != null ? externalDept.getUseYn() : "Y")
                .createdBy("SYSTEM")
                .build();

        departmentMapper.upsertByCode(department);
        log.debug("Department synced: code={}, name={}", departmentCode, externalDept.getDepartmentName());

        return true;
    }

    @Override
    @Transactional
    public SyncResult syncAllDepartments() {
        LocalDateTime startedAt = LocalDateTime.now();
        log.info("Starting full department synchronization (UPSERT mode)...");

        int created = 0;
        int updated = 0;
        int deactivated = 0;

        try {
            // 1. 외부 DB에서 모든 활성 부서 조회
            List<Department> externalDepts = externalDepartmentMapper.findAllFlat("Y");
            log.info("Found {} active departments in external DB", externalDepts.size());

            if (externalDepts.isEmpty()) {
                return SyncResult.success("DEPARTMENT", 0, 0, 0, 0, startedAt);
            }

            // 2. 상위 부서 먼저 처리하도록 정렬 (PARENT_CODE가 null인 것 먼저)
            List<Department> sortedDepts = externalDepts.stream()
                    .sorted((d1, d2) -> {
                        if (d1.getParentCode() == null && d2.getParentCode() != null) return -1;
                        if (d1.getParentCode() != null && d2.getParentCode() == null) return 1;
                        return 0;
                    })
                    .collect(Collectors.toList());

            // 3. 각 부서 UPSERT
            for (Department externalDept : sortedDepts) {
                // 기존 데이터 존재 여부 확인 (통계용)
                boolean exists = departmentMapper.existsByCode(externalDept.getDepartmentCode());

                Department department = Department.builder()
                        .departmentCode(externalDept.getDepartmentCode())
                        .departmentName(externalDept.getDepartmentName())
                        .parentCode(externalDept.getParentCode())
                        .sortOrder(externalDept.getSortOrder() != null ? externalDept.getSortOrder() : 0)
                        .useYn(externalDept.getUseYn() != null ? externalDept.getUseYn() : "Y")
                        .createdBy("SYSTEM")
                        .build();

                departmentMapper.upsertByCode(department);

                if (exists) {
                    updated++;
                } else {
                    created++;
                }

                log.debug("Synced department: {} ({})", externalDept.getDepartmentCode(), externalDept.getDepartmentName());
            }

            // 4. 동기화 대상이 아닌 부서 비활성화
            List<String> activeCodes = externalDepts.stream()
                    .map(Department::getDepartmentCode)
                    .collect(Collectors.toList());

            if (!activeCodes.isEmpty()) {
                deactivated = departmentMapper.deactivateDepartmentsNotIn(activeCodes);
                if (deactivated > 0) {
                    log.info("Deactivated {} departments not in external DB", deactivated);
                }
            }

            log.info("Department sync completed: created={}, updated={}, deactivated={}",
                    created, updated, deactivated);

            return SyncResult.success("DEPARTMENT", created, updated, deactivated, 0, startedAt);

        } catch (Exception e) {
            log.error("Department sync failed", e);
            return SyncResult.failure("DEPARTMENT", e.getMessage(), startedAt);
        }
    }
}
