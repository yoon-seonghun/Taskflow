package com.taskflow.service.impl;

import com.taskflow.domain.Item;
import com.taskflow.dto.item.AllItemResponse;
import com.taskflow.dto.item.AllItemsPageResponse;
import com.taskflow.dto.item.AllItemsSearchRequest;
import com.taskflow.dto.item.AllItemsStats;
import com.taskflow.mapper.ItemMapper;
import com.taskflow.service.AllItemsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 전체 업무 서비스 구현체 (v2.1)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AllItemsServiceImpl implements AllItemsService {

    private final ItemMapper itemMapper;

    @Override
    public AllItemsPageResponse getAllItems(String username, AllItemsSearchRequest request) {
        log.debug("전체 업무 조회: username={}, request={}", username, request);

        // 전체 업무 목록 조회
        List<Item> items = itemMapper.findAllItems(username, request);

        // 전체 업무 개수 조회
        long totalElements = itemMapper.countAllItems(username, request);

        // 통계 조회
        AllItemsStats stats = getStats(username, request);

        // 응답 변환
        List<AllItemResponse> content = AllItemResponse.fromList(items);

        return AllItemsPageResponse.of(
                content,
                request.getSafePage(),
                request.getSafeSize(),
                totalElements,
                stats
        );
    }

    @Override
    public AllItemsStats getStats(String username, AllItemsSearchRequest request) {
        log.debug("전체 업무 통계 조회: username={}", username);

        List<Map<String, Object>> rawStats = itemMapper.getItemsStats(username, request);

        // 통계 집계
        int totalCount = 0;
        int ownedCount = 0;
        int boardSharedCount = 0;
        int itemSharedCount = 0;
        int assignedCount = 0;
        int rootTaskCount = 0;
        int childTaskCount = 0;

        Map<String, Integer> byStatus = new HashMap<>();
        Map<String, Integer> byPriority = new HashMap<>();

        for (Map<String, Object> row : rawStats) {
            String sourceType = (String) row.get("SOURCE_TYPE");
            String status = (String) row.get("STATUS");
            String priority = (String) row.get("PRIORITY");
            int depth = row.get("ITEM_DEPTH") != null ? ((Number) row.get("ITEM_DEPTH")).intValue() : 0;
            int count = row.get("CNT") != null ? ((Number) row.get("CNT")).intValue() : 0;

            totalCount += count;

            // 출처별 집계
            switch (sourceType) {
                case "OWNED" -> ownedCount += count;
                case "BOARD_SHARED" -> boardSharedCount += count;
                case "ITEM_SHARED" -> itemSharedCount += count;
                case "ASSIGNED" -> assignedCount += count;
            }

            // 깊이별 집계
            if (depth == 0) {
                rootTaskCount += count;
            } else {
                childTaskCount += count;
            }

            // 상태별 집계
            if (status != null) {
                byStatus.merge(status, count, Integer::sum);
            }

            // 우선순위별 집계
            if (priority != null) {
                byPriority.merge(priority, count, Integer::sum);
            }
        }

        return AllItemsStats.builder()
                .totalCount(totalCount)
                .ownedCount(ownedCount)
                .boardSharedCount(boardSharedCount)
                .itemSharedCount(itemSharedCount)
                .assignedCount(assignedCount)
                .rootTaskCount(rootTaskCount)
                .childTaskCount(childTaskCount)
                .byStatus(byStatus)
                .byPriority(byPriority)
                .build();
    }
}
