package com.taskflow.dto.item;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 아이템 페이징 응답 DTO
 */
@Getter
@Builder
public class ItemPageResponse {

    /**
     * 아이템 목록
     */
    private List<ItemResponse> content;

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
     * 페이징 응답 생성
     */
    public static ItemPageResponse of(List<ItemResponse> content, int page, int size, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / size);

        return ItemPageResponse.builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .first(page == 0)
                .last(page >= totalPages - 1)
                .numberOfElements(content.size())
                .empty(content.isEmpty())
                .build();
    }
}
