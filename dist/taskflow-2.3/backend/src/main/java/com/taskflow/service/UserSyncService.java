package com.taskflow.service;

import com.taskflow.domain.User;
import com.taskflow.dto.sync.SyncResult;

/**
 * 사용자 동기화 서비스 인터페이스
 *
 * External 모드에서 외부 DB의 사용자 정보를 내부 TB_USER에 동기화
 *
 * USERNAME/DEPARTMENT_CODE 기반 FK 참조 시스템:
 * - USERNAME 기준 UPSERT 방식
 * - 외부/내부 ID 매핑 불필요
 */
public interface UserSyncService {

    /**
     * 로그인 시점 동기화 (단일 사용자)
     *
     * 외부 사용자 정보를 조회하여 USERNAME 기준 UPSERT
     * 부서 정보도 함께 동기화 (DEPARTMENT_CODE 기준)
     *
     * @param username 로그인 아이디
     * @return 동기화된 사용자 정보 (null이면 외부에 없음)
     */
    User syncUserOnLogin(String username);

    /**
     * 전체 사용자 동기화 (배치 모드)
     *
     * 외부 DB의 모든 사용자를 조회하여:
     * - USERNAME 기준 UPSERT (ON DUPLICATE KEY UPDATE)
     * - 동기화 대상이 아닌 사용자: USE_YN='N' 처리 (admin 제외)
     *
     * @return 동기화 결과
     */
    SyncResult syncAllUsers();

    /**
     * 사용자 정보 조회 (병합)
     *
     * 내부 사용자 정보 반환
     * External 모드에서도 내부 TB_USER에 저장된 정보 사용
     *
     * @param username 사용자 아이디
     * @return 사용자 정보
     */
    User getUserByUsername(String username);
}
