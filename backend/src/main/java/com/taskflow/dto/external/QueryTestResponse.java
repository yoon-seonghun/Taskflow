package com.taskflow.dto.external;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * 쿼리 테스트 응답 DTO
 */
@Getter
@Builder
public class QueryTestResponse {

    /**
     * 성공 여부
     */
    private boolean success;

    /**
     * 메시지 (성공/실패 상세)
     */
    private String message;

    /**
     * 실행 소요 시간 (ms)
     */
    private Long executionTimeMs;

    /**
     * 결과 행 수
     */
    private Integer rowCount;

    /**
     * 결과 컬럼 목록
     */
    private List<String> columns;

    /**
     * 결과 데이터 (미리보기용, 최대 100건)
     */
    private List<Map<String, Object>> data;

    /**
     * 매핑된 옵션 미리보기 (value/label/color 매핑 결과)
     */
    private List<QueryOptionPreview> options;

    /**
     * 성공 응답 생성
     */
    public static QueryTestResponse success(
            List<String> columns,
            List<Map<String, Object>> data,
            List<QueryOptionPreview> options,
            long executionTimeMs
    ) {
        return QueryTestResponse.builder()
                .success(true)
                .message("쿼리 실행 성공")
                .columns(columns)
                .data(data)
                .options(options)
                .rowCount(data != null ? data.size() : 0)
                .executionTimeMs(executionTimeMs)
                .build();
    }

    /**
     * 실패 응답 생성
     */
    public static QueryTestResponse failure(String errorMessage) {
        return QueryTestResponse.builder()
                .success(false)
                .message(errorMessage)
                .build();
    }

    /**
     * 실패 응답 생성 (소요시간 포함)
     */
    public static QueryTestResponse failure(String errorMessage, long executionTimeMs) {
        return QueryTestResponse.builder()
                .success(false)
                .message(errorMessage)
                .executionTimeMs(executionTimeMs)
                .build();
    }

    /**
     * 옵션 미리보기 DTO
     */
    @Getter
    @Builder
    public static class QueryOptionPreview {
        private String value;
        private String label;
        private String color;

        public static QueryOptionPreview of(String value, String label, String color) {
            return QueryOptionPreview.builder()
                    .value(value)
                    .label(label)
                    .color(color)
                    .build();
        }
    }
}
