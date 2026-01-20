package com.taskflow.mapper;

import com.taskflow.domain.ItemHistory;
import com.taskflow.dto.history.ItemHistorySearchRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 아이템 이력 Mapper
 */
@Mapper
public interface ItemHistoryMapper {

    /**
     * 작업 처리 이력 조회 (완료/삭제된 아이템)
     *
     * @param request 검색 조건
     * @return 이력 목록
     */
    List<ItemHistory> findItemHistory(@Param("request") ItemHistorySearchRequest request);

    /**
     * 작업 처리 이력 총 개수 조회
     *
     * @param request 검색 조건
     * @return 총 개수
     */
    long countItemHistory(@Param("request") ItemHistorySearchRequest request);
}
