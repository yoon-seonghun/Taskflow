package com.taskflow.service;

import com.taskflow.domain.PropertyDef;

import java.util.List;

/**
 * 속성 정의 캐시 서비스 인터페이스
 *
 * 보드별 속성 정의를 캐싱하여 성능 최적화
 * - 앱 초기화 시 속성 정의 로드
 * - 속성 변경 시 캐시 무효화
 */
public interface PropertyCacheService {

    /**
     * 보드별 속성 정의 목록 조회 (캐시 적용)
     *
     * @param boardId 보드 ID
     * @return 속성 정의 목록 (옵션 포함)
     */
    List<PropertyDef> getPropertiesByBoardId(Long boardId);

    /**
     * 보드의 속성 캐시 무효화
     *
     * @param boardId 보드 ID
     */
    void evictBoardCache(Long boardId);

    /**
     * 전체 캐시 무효화
     */
    void evictAllCache();

    /**
     * 보드의 속성 캐시 갱신
     *
     * @param boardId 보드 ID
     */
    void refreshBoardCache(Long boardId);
}
