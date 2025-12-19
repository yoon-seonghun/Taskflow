package com.taskflow.common;

import lombok.Getter;
import lombok.Setter;

/**
 * 페이징 요청 파라미터
 *
 * 쿼리 파라미터:
 * - page: 페이지 번호 (0부터 시작, 기본값 0)
 * - size: 페이지 크기 (기본값 20, 최대 100)
 * - sort: 정렬 필드,방향 (예: createdAt,desc)
 */
@Getter
@Setter
public class PageRequest {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    /**
     * 페이지 번호 (0부터 시작)
     */
    private int page = DEFAULT_PAGE;

    /**
     * 페이지 크기
     */
    private int size = DEFAULT_SIZE;

    /**
     * 정렬 필드
     */
    private String sortField;

    /**
     * 정렬 방향 (asc, desc)
     */
    private String sortDirection = "desc";

    public void setPage(int page) {
        this.page = Math.max(0, page);
    }

    public void setSize(int size) {
        this.size = Math.min(Math.max(1, size), MAX_SIZE);
    }

    /**
     * SQL LIMIT 시작 위치 (offset)
     */
    public int getOffset() {
        return page * size;
    }

    /**
     * SQL LIMIT 크기
     */
    public int getLimit() {
        return size;
    }

    /**
     * 정렬 방향이 내림차순인지 확인
     */
    public boolean isDescending() {
        return "desc".equalsIgnoreCase(sortDirection);
    }

    /**
     * 정렬 문자열 파싱 (예: "createdAt,desc")
     */
    public void setSort(String sort) {
        if (sort != null && !sort.isEmpty()) {
            String[] parts = sort.split(",");
            this.sortField = parts[0].trim();
            if (parts.length > 1) {
                this.sortDirection = parts[1].trim();
            }
        }
    }

    /**
     * 기본 PageRequest 생성
     */
    public static PageRequest of(int page, int size) {
        PageRequest request = new PageRequest();
        request.setPage(page);
        request.setSize(size);
        return request;
    }

    /**
     * 정렬 포함 PageRequest 생성
     */
    public static PageRequest of(int page, int size, String sortField, String sortDirection) {
        PageRequest request = of(page, size);
        request.setSortField(sortField);
        request.setSortDirection(sortDirection);
        return request;
    }
}
