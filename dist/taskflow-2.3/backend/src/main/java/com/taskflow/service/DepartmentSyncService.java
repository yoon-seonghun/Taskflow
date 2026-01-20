package com.taskflow.service;

import com.taskflow.dto.sync.SyncResult;

/**
 * 부서 동기화 서비스 인터페이스
 *
 * External 모드에서 외부 DB의 부서 정보를 내부 TB_DEPARTMENT에 동기화
 *
 * USERNAME/DEPARTMENT_CODE 기반 FK 참조 시스템:
 * - DEPARTMENT_CODE 기준 UPSERT 방식
 * - 외부/내부 ID 매핑 불필요
 */
public interface DepartmentSyncService {

    /**
     * 단일 부서 동기화 (로그인 시 사용)
     *
     * 외부 부서 정보를 조회하여 DEPARTMENT_CODE 기준 UPSERT
     *
     * @param departmentCode 부서 코드
     * @return 동기화 성공 여부
     */
    boolean syncDepartmentByCode(String departmentCode);

    /**
     * 전체 부서 동기화 (배치 모드)
     *
     * 외부 DB의 모든 부서를 조회하여:
     * - DEPARTMENT_CODE 기준 UPSERT (ON DUPLICATE KEY UPDATE)
     * - 동기화 대상이 아닌 부서: USE_YN='N' 처리
     *
     * @return 동기화 결과
     */
    SyncResult syncAllDepartments();
}
