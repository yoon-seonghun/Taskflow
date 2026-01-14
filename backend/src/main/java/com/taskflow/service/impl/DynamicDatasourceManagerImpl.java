package com.taskflow.service.impl;

import com.taskflow.common.enums.DbType;
import com.taskflow.domain.ExternalDatasource;
import com.taskflow.dto.external.ConnectionTestResponse;
import com.taskflow.service.DynamicDatasourceManager;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 외부 DB 동적 DataSource 관리 서비스 구현체
 */
@Slf4j
@Service
public class DynamicDatasourceManagerImpl implements DynamicDatasourceManager {

    private final ConcurrentHashMap<Long, HikariDataSource> datasourcePool = new ConcurrentHashMap<>();

    @Value("${external-query.default-query-timeout:30000}")
    private int defaultQueryTimeout;

    @Value("${external-query.max-result-rows:1000}")
    private int maxResultRows;

    @Override
    public DataSource createOrUpdateDatasource(ExternalDatasource datasource, String decryptedPassword) {
        Long id = datasource.getDatasourceId();

        // 기존 풀이 있으면 종료
        if (datasourcePool.containsKey(id)) {
            removeDatasource(id);
        }

        // 새 풀 생성
        HikariDataSource hikariDataSource = createHikariDataSource(datasource, decryptedPassword);
        datasourcePool.put(id, hikariDataSource);

        log.info("DataSource created/updated: {} ({})", datasource.getDatasourceCode(), id);
        return hikariDataSource;
    }

    @Override
    public DataSource getDatasource(Long datasourceId) {
        return datasourcePool.get(datasourceId);
    }

    @Override
    public boolean hasDatasource(Long datasourceId) {
        return datasourcePool.containsKey(datasourceId);
    }

    @Override
    public void removeDatasource(Long datasourceId) {
        HikariDataSource ds = datasourcePool.remove(datasourceId);
        if (ds != null && !ds.isClosed()) {
            try {
                ds.close();
                log.info("DataSource closed: {}", datasourceId);
            } catch (Exception e) {
                log.warn("Failed to close DataSource: {}", datasourceId, e);
            }
        }
    }

    @Override
    public void removeAllDatasources() {
        log.info("Closing all external DataSources...");
        datasourcePool.forEach((id, ds) -> {
            if (!ds.isClosed()) {
                try {
                    ds.close();
                } catch (Exception e) {
                    log.warn("Failed to close DataSource: {}", id, e);
                }
            }
        });
        datasourcePool.clear();
        log.info("All external DataSources closed");
    }

    @Override
    public ConnectionTestResponse testConnection(ExternalDatasource datasource, String decryptedPassword) {
        long startTime = System.currentTimeMillis();

        try {
            HikariConfig config = buildHikariConfig(datasource, decryptedPassword);
            config.setMaximumPoolSize(1);
            config.setMinimumIdle(0);
            config.setConnectionTimeout(datasource.getConnectionTimeout() != null ?
                    datasource.getConnectionTimeout() : 5000);

            try (HikariDataSource testDs = new HikariDataSource(config);
                 Connection conn = testDs.getConnection()) {

                DatabaseMetaData metaData = conn.getMetaData();
                String dbVersion = metaData.getDatabaseProductName() + " " + metaData.getDatabaseProductVersion();
                String driverInfo = metaData.getDriverName() + " " + metaData.getDriverVersion();

                long connectionTime = System.currentTimeMillis() - startTime;
                return ConnectionTestResponse.success(dbVersion, driverInfo, connectionTime);
            }
        } catch (SQLException e) {
            long connectionTime = System.currentTimeMillis() - startTime;
            log.warn("Connection test failed for {}: {}", datasource.getDatasourceCode(), e.getMessage());
            return ConnectionTestResponse.failure("연결 실패: " + e.getMessage(), connectionTime);
        } catch (Exception e) {
            long connectionTime = System.currentTimeMillis() - startTime;
            log.error("Unexpected error during connection test for {}", datasource.getDatasourceCode(), e);
            return ConnectionTestResponse.failure("오류 발생: " + e.getMessage(), connectionTime);
        }
    }

    @Override
    public List<Map<String, Object>> executeQuery(Long datasourceId, String sql, int maxRows) {
        HikariDataSource ds = datasourcePool.get(datasourceId);
        if (ds == null) {
            throw new IllegalStateException("DataSource not found: " + datasourceId);
        }

        int effectiveMaxRows = Math.min(maxRows, maxResultRows);
        List<Map<String, Object>> results = new ArrayList<>();

        try (Connection conn = ds.getConnection()) {
            conn.setReadOnly(true);
            conn.setAutoCommit(false);

            try (Statement stmt = conn.createStatement()) {
                stmt.setQueryTimeout(defaultQueryTimeout / 1000);
                stmt.setMaxRows(effectiveMaxRows);

                try (ResultSet rs = stmt.executeQuery(sql)) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    int columnCount = metaData.getColumnCount();

                    while (rs.next()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (int i = 1; i <= columnCount; i++) {
                            String columnName = metaData.getColumnLabel(i);
                            Object value = rs.getObject(i);
                            row.put(columnName, value);
                        }
                        results.add(row);
                    }
                }
            } finally {
                conn.rollback(); // 읽기 전용이지만 명시적 롤백
            }
        } catch (SQLException e) {
            log.error("Query execution failed on datasource {}: {}", datasourceId, e.getMessage());
            throw new RuntimeException("쿼리 실행 실패: " + e.getMessage(), e);
        }

        return results;
    }

    @Override
    public String buildJdbcUrl(ExternalDatasource datasource) {
        String dbType = datasource.getDbType();

        return switch (dbType) {
            case ExternalDatasource.DB_TYPE_MYSQL -> buildMySqlUrl(datasource);
            case ExternalDatasource.DB_TYPE_ORACLE -> buildOracleUrl(datasource);
            case ExternalDatasource.DB_TYPE_MSSQL -> buildMsSqlUrl(datasource);
            case ExternalDatasource.DB_TYPE_TIBERO -> buildTiberoUrl(datasource);
            default -> throw new IllegalArgumentException("Unsupported DB type: " + dbType);
        };
    }

    @Override
    public String getDriverClassName(String dbType) {
        return switch (dbType) {
            case ExternalDatasource.DB_TYPE_MYSQL -> DbType.MYSQL.getDriverClassName();
            case ExternalDatasource.DB_TYPE_ORACLE -> DbType.ORACLE.getDriverClassName();
            case ExternalDatasource.DB_TYPE_MSSQL -> DbType.MSSQL.getDriverClassName();
            case ExternalDatasource.DB_TYPE_TIBERO -> DbType.TIBERO.getDriverClassName();
            default -> throw new IllegalArgumentException("Unsupported DB type: " + dbType);
        };
    }

    @Override
    public String getValidationQuery(String dbType) {
        return switch (dbType) {
            case ExternalDatasource.DB_TYPE_MYSQL -> DbType.MYSQL.getValidationQuery();
            case ExternalDatasource.DB_TYPE_ORACLE -> DbType.ORACLE.getValidationQuery();
            case ExternalDatasource.DB_TYPE_MSSQL -> DbType.MSSQL.getValidationQuery();
            case ExternalDatasource.DB_TYPE_TIBERO -> DbType.TIBERO.getValidationQuery();
            default -> "SELECT 1";
        };
    }

    // =============================================
    // Private Methods
    // =============================================

    private HikariDataSource createHikariDataSource(ExternalDatasource datasource, String decryptedPassword) {
        HikariConfig config = buildHikariConfig(datasource, decryptedPassword);
        return new HikariDataSource(config);
    }

    private HikariConfig buildHikariConfig(ExternalDatasource datasource, String decryptedPassword) {
        HikariConfig config = new HikariConfig();

        // 기본 설정
        config.setJdbcUrl(buildJdbcUrl(datasource));
        config.setUsername(datasource.getUsername());
        config.setPassword(decryptedPassword);
        config.setDriverClassName(getDriverClassName(datasource.getDbType()));

        // 풀 설정
        int maxPoolSize = datasource.getMaxPoolSize() != null ? datasource.getMaxPoolSize() : 3;
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(datasource.getConnectionTimeout() != null ?
                datasource.getConnectionTimeout() : 5000);
        config.setIdleTimeout(60000); // 1분
        config.setMaxLifetime(300000); // 5분

        // 읽기 전용 설정 (보안)
        config.setReadOnly(true);
        config.setAutoCommit(false);

        // 유효성 검사
        config.setConnectionTestQuery(getValidationQuery(datasource.getDbType()));

        // 풀 이름
        config.setPoolName("External-" + datasource.getDatasourceCode());

        return config;
    }

    private String buildMySqlUrl(ExternalDatasource datasource) {
        return String.format(
                "jdbc:mysql://%s:%d/%s?useSSL=false&serverTimezone=Asia/Seoul" +
                        "&useUnicode=true&characterEncoding=UTF-8&allowPublicKeyRetrieval=true",
                datasource.getHost(),
                datasource.getPort(),
                datasource.getDatabaseName()
        );
    }

    private String buildOracleUrl(ExternalDatasource datasource) {
        // SID vs Service Name 방식 분기
        if ("SID".equals(datasource.getConnectionType())) {
            return String.format(
                    "jdbc:oracle:thin:@%s:%d:%s",
                    datasource.getHost(),
                    datasource.getPort(),
                    datasource.getDatabaseName()
            );
        }
        // 기본: Service Name 방식
        return String.format(
                "jdbc:oracle:thin:@//%s:%d/%s",
                datasource.getHost(),
                datasource.getPort(),
                datasource.getDatabaseName()
        );
    }

    private String buildMsSqlUrl(ExternalDatasource datasource) {
        return String.format(
                "jdbc:sqlserver://%s:%d;databaseName=%s;encrypt=false;trustServerCertificate=true",
                datasource.getHost(),
                datasource.getPort(),
                datasource.getDatabaseName()
        );
    }

    private String buildTiberoUrl(ExternalDatasource datasource) {
        return String.format(
                "jdbc:tibero:thin:@%s:%d:%s",
                datasource.getHost(),
                datasource.getPort(),
                datasource.getDatabaseName()
        );
    }
}
