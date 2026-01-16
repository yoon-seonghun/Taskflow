package com.taskflow.dto.item;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * 전체 업무 통계 DTO (v2.1)
 */
@Getter
@Builder
public class AllItemsStats {

    /**
     * 전체 업무 수
     */
    private int totalCount;

    /**
     * 소유 보드 업무 수
     */
    private int ownedCount;

    /**
     * 공유 보드 업무 수
     */
    private int boardSharedCount;

    /**
     * 개별 공유 업무 수
     */
    private int itemSharedCount;

    /**
     * 배당받은 업무 수 (v2.1)
     */
    private int assignedCount;

    /**
     * 기본 업무 수 (depth=0)
     */
    private int rootTaskCount;

    /**
     * 하위 업무 수 (depth>0)
     */
    private int childTaskCount;

    /**
     * 상태별 개수
     */
    private Map<String, Integer> byStatus;

    /**
     * 우선순위별 개수
     */
    private Map<String, Integer> byPriority;

    /**
     * 빈 통계 생성
     */
    public static AllItemsStats empty() {
        return AllItemsStats.builder()
                .totalCount(0)
                .ownedCount(0)
                .boardSharedCount(0)
                .itemSharedCount(0)
                .assignedCount(0)
                .rootTaskCount(0)
                .childTaskCount(0)
                .byStatus(Map.of())
                .byPriority(Map.of())
                .build();
    }
}
