package com.taskflow.service.impl;

import com.taskflow.service.QueryValidationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * 외부 쿼리 검증 서비스 구현체
 */
@Slf4j
@Service
public class QueryValidationServiceImpl implements QueryValidationService {

    /**
     * 금지 키워드 목록
     */
    private static final Set<String> DANGEROUS_KEYWORDS = Set.of(
            "INSERT", "UPDATE", "DELETE", "DROP", "CREATE", "ALTER", "TRUNCATE",
            "GRANT", "REVOKE", "EXECUTE", "EXEC", "XP_", "SP_",
            "INTO OUTFILE", "INTO DUMPFILE", "LOAD_FILE", "LOAD DATA",
            "CALL", "MERGE", "UPSERT", "REPLACE"
    );

    /**
     * SQL 주석 패턴
     */
    private static final Pattern SINGLE_LINE_COMMENT = Pattern.compile("--.*$", Pattern.MULTILINE);
    private static final Pattern MULTI_LINE_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);

    /**
     * 공백 정규화 패턴
     */
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    /**
     * 문자열 리터럴 패턴 (검증 시 제외용)
     */
    private static final Pattern STRING_LITERAL = Pattern.compile("'[^']*'|\"[^\"]*\"");

    @Override
    public ValidationResult validateQuery(String sql) {
        if (sql == null || sql.isBlank()) {
            return ValidationResult.failure("SQL 쿼리가 비어있습니다");
        }

        // 1. 정규화
        String normalizedSql = normalizeQuery(sql);

        // 2. SELECT 문 체크
        if (!isSelectQuery(normalizedSql)) {
            return ValidationResult.failure("SELECT 문만 허용됩니다");
        }

        // 3. 다중 쿼리 체크
        if (containsMultipleStatements(normalizedSql)) {
            return ValidationResult.failure("다중 쿼리는 허용되지 않습니다");
        }

        // 4. 위험 키워드 체크
        if (containsDangerousKeywords(normalizedSql)) {
            return ValidationResult.failure("허용되지 않는 SQL 키워드가 포함되어 있습니다");
        }

        return ValidationResult.success(normalizedSql);
    }

    @Override
    public boolean isSelectQuery(String sql) {
        String normalized = normalizeQuery(sql);
        return normalized.toUpperCase().startsWith("SELECT ");
    }

    @Override
    public boolean containsDangerousKeywords(String sql) {
        // 문자열 리터럴 제거 후 검사 (SQL 내 문자열에 키워드가 있는 경우 허용)
        String sqlWithoutStrings = STRING_LITERAL.matcher(sql).replaceAll("''");
        String upperSql = sqlWithoutStrings.toUpperCase();

        for (String keyword : DANGEROUS_KEYWORDS) {
            // 단어 경계 체크
            String pattern = "\\b" + Pattern.quote(keyword) + "\\b";
            if (Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(upperSql).find()) {
                log.warn("Dangerous keyword detected: {} in query", keyword);
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsMultipleStatements(String sql) {
        // 문자열 리터럴 제거 후 세미콜론 체크
        String sqlWithoutStrings = STRING_LITERAL.matcher(sql).replaceAll("''");

        // 세미콜론으로 분리된 복수 구문 감지
        // 마지막 세미콜론은 허용 (SELECT ... ;)
        String trimmed = sqlWithoutStrings.trim();
        if (trimmed.endsWith(";")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }

        return trimmed.contains(";");
    }

    @Override
    public String normalizeQuery(String sql) {
        if (sql == null) {
            return "";
        }

        String normalized = sql;

        // 1. 주석 제거
        normalized = SINGLE_LINE_COMMENT.matcher(normalized).replaceAll("");
        normalized = MULTI_LINE_COMMENT.matcher(normalized).replaceAll("");

        // 2. 공백 정규화
        normalized = WHITESPACE.matcher(normalized).replaceAll(" ");

        // 3. 앞뒤 공백 제거
        normalized = normalized.trim();

        return normalized;
    }
}
