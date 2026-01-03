package com.taskflow.service;

import com.taskflow.dto.sync.SyncResult;

/**
 * 직급 동기화 서비스 인터페이스
 *
 * External 모드에서 외부 DB의 직급 정보를 내부 TB_POSITION에 동기화
 *
 * POSITION_CODE 기반 FK 참조 시스템:
 * - POSITION_CODE 기준 UPSERT 방식
 * - 외부/내부 ID 매핑 불필요
 */
public interface PositionSyncService {

    /**
     * 단일 직급 동기화 (필요 시 사용)
     *
     * 외부 직급 정보를 조회하여 POSITION_CODE 기준 UPSERT
     *
     * @param positionCode 직급 코드
     * @return 동기화 성공 여부
     */
    boolean syncPositionByCode(String positionCode);

    /**
     * 전체 직급 동기화 (배치 모드)
     *
     * 외부 DB의 모든 직급을 조회하여:
     * - POSITION_CODE 기준 UPSERT (ON DUPLICATE KEY UPDATE)
     * - 동기화 대상이 아닌 직급: USE_YN='N' 처리
     *
     * @return 동기화 결과
     */
    SyncResult syncAllPositions();
}
