package com.taskflow.dto.item;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 전체 업무 페이징 응답 DTO (v2.1)
 * - 통계 정보 포함
 */
@Getter
@Builder
public class AllItemsPageResponse {

    /**
     * 업무 목록
     */
    private List<AllItemResponse> content;

    /**
     * 현재 페이지 번호 (0부터 시작)
     */
    private int page;

    /**
     * 페이지 크기
     */
    private int size;

    /**
     * 전체 아이템 수
     */
    private long totalElements;

    /**
     * 전체 페이지 수
     */
    private int totalPages;

    /**
     * 첫 페이지 여부
     */
    private boolean first;

    /**
     * 마지막 페이지 여부
     */
    private boolean last;

    /**
     * 현재 페이지 아이템 수
     */
    private int numberOfElements;

    /**
     * 빈 페이지 여부
     */
    private boolean empty;

    /**
     * 통계 정보 (v2.1)
     */
    private AllItemsStats stats;

    /**
     * 페이징 응답 생성
     */
    public static AllItemsPageResponse of(List<AllItemResponse> content, int page, int size,
                                          long totalElements, AllItemsStats stats) {
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;

        return AllItemsPageResponse.builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .first(page == 0)
                .last(totalPages == 0 || page >= totalPages - 1)
                .numberOfElements(content.size())
                .empty(content.isEmpty())
                .stats(stats)
                .build();
    }
}
