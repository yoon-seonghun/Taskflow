package com.taskflow.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 지원하는 외부 DB 타입
 */
@Getter
@RequiredArgsConstructor
public enum DbType {

    MYSQL("MySQL", "com.mysql.cj.jdbc.Driver", 3306, "SELECT 1"),
    ORACLE("Oracle", "oracle.jdbc.OracleDriver", 1521, "SELECT 1 FROM DUAL"),
    MSSQL("SQL Server", "com.microsoft.sqlserver.jdbc.SQLServerDriver", 1433, "SELECT 1"),
    TIBERO("Tibero", "com.tmax.tibero.jdbc.TbDriver", 8629, "SELECT 1 FROM DUAL");

    private final String displayName;
    private final String driverClassName;
    private final int defaultPort;
    private final String validationQuery;

    /**
     * 문자열에서 DbType 변환
     */
    public static DbType fromString(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("DB type cannot be null or empty");
        }

        try {
            return DbType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported DB type: " + value);
        }
    }

    /**
     * 지원 여부 확인
     */
    public static boolean isSupported(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            DbType.valueOf(value.toUpperCase());
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
