package com.taskflow.service;

import com.taskflow.dto.external.QueryExecuteResponse.QueryOption;

import java.util.List;
import java.util.Optional;

/**
 * 외부 쿼리 결과 캐싱 서비스
 *
 * Caffeine 기반 TTL 캐싱을 제공합니다.
 */
public interface QueryCacheService {

    /**
     * 캐시에서 쿼리 결과 조회
     *
     * @param queryId 쿼리 ID
     * @return 캐시된 옵션 목록 (없으면 empty)
     */
    Optional<List<QueryOption>> get(Long queryId);

    /**
     * 쿼리 결과를 캐시에 저장
     *
     * @param queryId 쿼리 ID
     * @param options 옵션 목록
     * @param ttlSeconds TTL (초)
     */
    void put(Long queryId, List<QueryOption> options, int ttlSeconds);

    /**
     * 특정 쿼리의 캐시 무효화
     *
     * @param queryId 쿼리 ID
     */
    void evict(Long queryId);

    /**
     * 데이터소스에 속한 모든 쿼리 캐시 무효화
     *
     * @param datasourceId 데이터소스 ID
     */
    void evictByDatasource(Long datasourceId);

    /**
     * 모든 캐시 무효화
     */
    void evictAll();

    /**
     * 캐시 존재 여부 확인
     *
     * @param queryId 쿼리 ID
     * @return 캐시 존재 여부
     */
    boolean contains(Long queryId);

    /**
     * 캐시 통계 정보 조회
     *
     * @return 통계 정보
     */
    CacheStats getStats();

    /**
     * 캐시 통계 정보
     */
    record CacheStats(
            long size,
            long hitCount,
            long missCount,
            double hitRate
    ) {}
}
