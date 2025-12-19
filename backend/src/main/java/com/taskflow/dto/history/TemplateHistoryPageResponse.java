package com.taskflow.dto.history;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 작업 등록 이력 페이징 응답 DTO
 */
@Getter
@Builder
public class TemplateHistoryPageResponse {

    /**
     * 이력 목록
     */
    private List<TemplateHistoryResponse> content;

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
    public static TemplateHistoryPageResponse of(List<TemplateHistoryResponse> content, int page, int size, long totalElements) {
        int totalPages = (int) Math.ceil((double) totalElements / size);

        return TemplateHistoryPageResponse.builder()
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
