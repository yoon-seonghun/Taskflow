package com.taskflow.dto.external;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 쿼리 실행 응답 DTO (속성 옵션 조회용)
 */
@Getter
@Builder
public class QueryExecuteResponse {

    /**
     * 성공 여부
     */
    private boolean success;

    /**
     * 메시지
     */
    private String message;

    /**
     * 쿼리 ID
     */
    private Long queryId;

    /**
     * 쿼리 코드
     */
    private String queryCode;

    /**
     * 쿼리 이름
     */
    private String queryName;

    /**
     * 옵션 목록
     */
    private List<QueryOption> options;

    /**
     * 캐시 히트 여부
     */
    private boolean cached;

    /**
     * 마지막 실행 시간
     */
    private LocalDateTime lastExecutedAt;

    /**
     * 결과 건수
     */
    private Integer resultCount;

    /**
     * 성공 응답 생성
     */
    public static QueryExecuteResponse success(
            Long queryId,
            String queryCode,
            String queryName,
            List<QueryOption> options,
            boolean cached
    ) {
        return QueryExecuteResponse.builder()
                .success(true)
                .message("조회 성공")
                .queryId(queryId)
                .queryCode(queryCode)
                .queryName(queryName)
                .options(options)
                .resultCount(options != null ? options.size() : 0)
                .cached(cached)
                .lastExecutedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 실패 응답 생성
     */
    public static QueryExecuteResponse failure(String errorMessage) {
        return QueryExecuteResponse.builder()
                .success(false)
                .message(errorMessage)
                .options(Collections.emptyList())
                .resultCount(0)
                .cached(false)
                .build();
    }

    /**
     * 쿼리 옵션 DTO
     */
    @Getter
    @Builder
    public static class QueryOption {
        /**
         * 값 (저장용)
         */
        private String value;

        /**
         * 라벨 (표시용)
         */
        private String label;

        /**
         * 색상 (선택)
         */
        private String color;

        public static QueryOption of(String value, String label) {
            return QueryOption.builder()
                    .value(value)
                    .label(label)
                    .build();
        }

        public static QueryOption of(String value, String label, String color) {
            return QueryOption.builder()
                    .value(value)
                    .label(label)
                    .color(color)
                    .build();
        }
    }
}
