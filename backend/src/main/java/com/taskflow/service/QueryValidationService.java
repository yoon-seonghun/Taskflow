package com.taskflow.service;

/**
 * 외부 쿼리 검증 서비스
 *
 * SELECT 쿼리만 허용하고, 위험한 SQL 명령어를 필터링합니다.
 */
public interface QueryValidationService {

    /**
     * SQL 쿼리 검증
     *
     * @param sql SQL 쿼리
     * @return 검증 결과
     */
    ValidationResult validateQuery(String sql);

    /**
     * SELECT 문인지 확인
     *
     * @param sql SQL 쿼리
     * @return SELECT 문 여부
     */
    boolean isSelectQuery(String sql);

    /**
     * 위험한 키워드 포함 여부 확인
     *
     * @param sql SQL 쿼리
     * @return 위험 키워드 포함 여부
     */
    boolean containsDangerousKeywords(String sql);

    /**
     * 다중 쿼리(세미콜론) 포함 여부 확인
     *
     * @param sql SQL 쿼리
     * @return 다중 쿼리 포함 여부
     */
    boolean containsMultipleStatements(String sql);

    /**
     * 쿼리 정규화 (공백, 주석 제거)
     *
     * @param sql SQL 쿼리
     * @return 정규화된 쿼리
     */
    String normalizeQuery(String sql);

    /**
     * 검증 결과
     */
    record ValidationResult(
            boolean valid,
            String message,
            String normalizedSql
    ) {
        public static ValidationResult success(String normalizedSql) {
            return new ValidationResult(true, null, normalizedSql);
        }

        public static ValidationResult failure(String message) {
            return new ValidationResult(false, message, null);
        }
    }
}
