package com.taskflow.dto.history;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 작업 처리 이력 페이징 응답 DTO
 */
@Getter
@Builder
public class ItemHistoryPageResponse {

    /**
     * 이력 목록
     */
    private List<ItemHistoryResponse> content;

    /**
     * 현재 페이지 번호
     */
    private int page;

    /**
     * 페이지 크기
     */
    private int size;

    /**
     * 총 항목 수
     */
    private long totalElements;

    /**
     * 총 페이지 수
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
     * 페이징 응답 생성
     */
    public static ItemHistoryPageResponse of(List<ItemHistoryResponse> content, int page, int size, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / size);

        return ItemHistoryPageResponse.builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .first(page == 0)
                .last(page >= totalPages - 1)
                .build();
    }
}
