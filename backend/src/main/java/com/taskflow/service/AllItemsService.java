package com.taskflow.service;

import com.taskflow.dto.item.AllItemsPageResponse;
import com.taskflow.dto.item.AllItemsSearchRequest;
import com.taskflow.dto.item.AllItemsStats;

/**
 * 전체 업무 서비스 인터페이스 (v2.1)
 * - 소유 보드 + 공유 보드 + 개별 공유/배당 업무 통합 조회
 */
public interface AllItemsService {

    /**
     * 전체 업무 목록 조회 (페이징)
     *
     * @param username 사용자 USERNAME
     * @param request  검색 조건
     * @return 전체 업무 페이징 응답 (통계 포함)
     */
    AllItemsPageResponse getAllItems(String username, AllItemsSearchRequest request);

    /**
     * 전체 업무 통계 조회
     *
     * @param username 사용자 USERNAME
     * @param request  검색 조건 (필터만 적용)
     * @return 출처별/상태별/우선순위별 통계
     */
    AllItemsStats getStats(String username, AllItemsSearchRequest request);
}
