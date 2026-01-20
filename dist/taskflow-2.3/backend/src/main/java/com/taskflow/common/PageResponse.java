package com.taskflow.common;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 페이징 응답 래퍼
 *
 * @param <T> 데이터 타입
 */
@Getter
@Builder
public class PageResponse<T> {

    /**
     * 데이터 목록
     */
    private final List<T> content;

    /**
     * 현재 페이지 (0부터 시작)
     */
    private final int page;

    /**
     * 페이지 크기
     */
    private final int size;

    /**
     * 전체 데이터 수
     */
    private final long totalElements;

    /**
     * 전체 페이지 수
     */
    private final int totalPages;

    /**
     * 첫 페이지 여부
     */
    private final boolean first;

    /**
     * 마지막 페이지 여부
     */
    private final boolean last;

    /**
     * 빈 데이터 여부
     */
    private final boolean empty;

    /**
     * PageResponse 생성
     */
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / size);

        return PageResponse.<T>builder()
                .content(content)
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .first(page == 0)
                .last(page >= totalPages - 1)
                .empty(content.isEmpty())
                .build();
    }

    /**
     * 빈 PageResponse 생성
     */
    public static <T> PageResponse<T> empty(int page, int size) {
        return PageResponse.<T>builder()
                .content(List.of())
                .page(page)
                .size(size)
                .totalElements(0)
                .totalPages(0)
                .first(true)
                .last(true)
                .empty(true)
                .build();
    }
}
