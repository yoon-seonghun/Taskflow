package com.taskflow.service;

import com.taskflow.dto.history.*;

/**
 * 이력 서비스 인터페이스
 */
public interface HistoryService {

    /**
     * 작업 처리 이력 조회 (완료/삭제된 아이템)
     *
     * @param request 검색 조건
     * @return 페이징된 이력 목록
     */
    ItemHistoryPageResponse getItemHistory(ItemHistorySearchRequest request);

    /**
     * 작업 등록 이력 조회 (템플릿)
     *
     * @param request 검색 조건
     * @return 페이징된 이력 목록
     */
    TemplateHistoryPageResponse getTemplateHistory(TemplateHistorySearchRequest request);
}
