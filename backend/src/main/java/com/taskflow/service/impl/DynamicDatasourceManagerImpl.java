package com.taskflow.service.impl;

import com.taskflow.common.enums.DbType;
import com.taskflow.domain.ExternalDatasource;
import com.taskflow.dto.external.ConnectionTestResponse;
import com.taskflow.service.DynamicDatasourceManager;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 외부 DB 동적 DataSource 관리 서비스 구현체
 *
 * Tibero6/7 동적 JAR 로딩 지원:
 * - 같은 드라이버 클래스명(com.tmax.tibero.jdbc.TbDriver)을 사용하는
 *   두 버전의 JAR를 URLClassLoader로 격리하여 로드
 */
@Slf4j
@Service
public class DynamicDatasourceManagerImpl implements DynamicDatasourceManager {

    private final ConcurrentHashMap<Long, HikariDataSource> datasourcePool = new ConcurrentHashMap<>();

    // Tibero 드라이버 클래스로더 캐시
    private volatile URLClassLoader tibero6ClassLoader;
    private volatile URLClassLoader tibero7ClassLoader;

    // Tibero 드라이버 인스턴스 캐시
    private volatile Driver tibero6Driver;
    private volatile Driver tibero7Driver;

    @Value("${external-query.default-query-timeout:30000}")
    private int defaultQueryTimeout;

    @Value("${external-query.max-result-rows:1000}")
    private int maxResultRows;

    @Value("${external-query.tibero.tibero6-jar-path:lib/tibero/tibero6-jdbc.jar}")
    private String tibero6JarPath;

    @Value("${external-query.tibero.tibero7-jar-path:lib/tibero/tibero7-jdbc.jar}")
    private String tibero7JarPath;

    @Value("${external-query.tibero.driver-class-name:com.tmax.tibero.jdbc.TbDriver}")
    private String tiberoDriverClassName;

    @PostConstruct
    public void init() {
        log.info("DynamicDatasourceManager initialized");
        log.info("Tibero6 JAR path: {}", tibero6JarPath);
        log.info("Tibero7 JAR path: {}", tibero7JarPath);
    }

    @PreDestroy
    public void destroy() {
        removeAllDatasources();
        closeTiberoClassLoaders();
    }

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

        log.info("DataSource created/updated: {} ({}) - {}", datasource.getDatasourceCode(), id, datasource.getDbType());
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
            // Tibero는 직접 연결 테스트
            if (datasource.isTibero()) {
                return testTiberoConnection(datasource, decryptedPassword, startTime);
            }

            // 다른 DB는 HikariCP로 테스트
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

    /**
     * Tibero 연결 테스트 (동적 드라이버 로딩)
     */
    private ConnectionTestResponse testTiberoConnection(ExternalDatasource datasource, String decryptedPassword, long startTime) {
        try {
            Driver driver = getTiberoDriver(datasource.getDbType());
            if (driver == null) {
                return ConnectionTestResponse.failure("Tibero 드라이버를 로드할 수 없습니다.", System.currentTimeMillis() - startTime);
            }

            String jdbcUrl = buildJdbcUrl(datasource);
            Properties props = new Properties();
            props.setProperty("user", datasource.getUsername());
            props.setProperty("password", decryptedPassword);

            try (Connection conn = driver.connect(jdbcUrl, props)) {
                if (conn == null) {
                    return ConnectionTestResponse.failure("연결 실패: 드라이버가 URL을 처리할 수 없습니다.", System.currentTimeMillis() - startTime);
                }

                DatabaseMetaData metaData = conn.getMetaData();
                String dbVersion = metaData.getDatabaseProductName() + " " + metaData.getDatabaseProductVersion();
                String driverInfo = metaData.getDriverName() + " " + metaData.getDriverVersion();

                long connectionTime = System.currentTimeMillis() - startTime;
                return ConnectionTestResponse.success(dbVersion, driverInfo, connectionTime);
            }
        } catch (Exception e) {
            long connectionTime = System.currentTimeMillis() - startTime;
            log.warn("Tibero connection test failed for {}: {}", datasource.getDatasourceCode(), e.getMessage());
            return ConnectionTestResponse.failure("연결 실패: " + e.getMessage(), connectionTime);
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
                            // 컬럼명을 대문자로 정규화 (DB별 대소문자 차이 해결)
                            String columnName = metaData.getColumnLabel(i).toUpperCase();
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
            case ExternalDatasource.DB_TYPE_TIBERO6, ExternalDatasource.DB_TYPE_TIBERO7 -> buildTiberoUrl(datasource);
            default -> throw new IllegalArgumentException("Unsupported DB type: " + dbType);
        };
    }

    @Override
    public String getDriverClassName(String dbType) {
        return switch (dbType) {
            case ExternalDatasource.DB_TYPE_MYSQL -> DbType.MYSQL.getDriverClassName();
            case ExternalDatasource.DB_TYPE_ORACLE -> DbType.ORACLE.getDriverClassName();
            case ExternalDatasource.DB_TYPE_MSSQL -> DbType.MSSQL.getDriverClassName();
            case ExternalDatasource.DB_TYPE_TIBERO6 -> DbType.TIBERO6.getDriverClassName();
            case ExternalDatasource.DB_TYPE_TIBERO7 -> DbType.TIBERO7.getDriverClassName();
            default -> throw new IllegalArgumentException("Unsupported DB type: " + dbType);
        };
    }

    @Override
    public String getValidationQuery(String dbType) {
        return switch (dbType) {
            case ExternalDatasource.DB_TYPE_MYSQL -> DbType.MYSQL.getValidationQuery();
            case ExternalDatasource.DB_TYPE_ORACLE -> DbType.ORACLE.getValidationQuery();
            case ExternalDatasource.DB_TYPE_MSSQL -> DbType.MSSQL.getValidationQuery();
            case ExternalDatasource.DB_TYPE_TIBERO6 -> DbType.TIBERO6.getValidationQuery();
            case ExternalDatasource.DB_TYPE_TIBERO7 -> DbType.TIBERO7.getValidationQuery();
            default -> "SELECT 1";
        };
    }

    // =============================================
    // Tibero 동적 드라이버 로딩
    // =============================================

    /**
     * Tibero 드라이버 인스턴스 가져오기 (동적 로딩)
     */
    private Driver getTiberoDriver(String dbType) {
        try {
            if (ExternalDatasource.DB_TYPE_TIBERO6.equals(dbType)) {
                if (tibero6Driver == null) {
                    synchronized (this) {
                        if (tibero6Driver == null) {
                            tibero6Driver = loadTiberoDriver(tibero6JarPath, "TIBERO6");
                        }
                    }
                }
                return tibero6Driver;
            } else if (ExternalDatasource.DB_TYPE_TIBERO7.equals(dbType)) {
                if (tibero7Driver == null) {
                    synchronized (this) {
                        if (tibero7Driver == null) {
                            tibero7Driver = loadTiberoDriver(tibero7JarPath, "TIBERO7");
                        }
                    }
                }
                return tibero7Driver;
            }
            return null;
        } catch (Exception e) {
            log.error("Failed to get Tibero driver for {}: {}", dbType, e.getMessage());
            return null;
        }
    }

    /**
     * Tibero JDBC 드라이버 동적 로드
     */
    private Driver loadTiberoDriver(String jarPath, String version) throws Exception {
        File jarFile = new File(jarPath);
        if (!jarFile.exists()) {
            log.error("{} JAR file not found: {}", version, jarFile.getAbsolutePath());
            throw new IllegalStateException(version + " JAR 파일을 찾을 수 없습니다: " + jarFile.getAbsolutePath());
        }

        URL jarUrl = jarFile.toURI().toURL();
        URLClassLoader classLoader = new URLClassLoader(new URL[]{jarUrl}, this.getClass().getClassLoader());

        // 클래스로더 캐시
        if ("TIBERO6".equals(version)) {
            tibero6ClassLoader = classLoader;
        } else {
            tibero7ClassLoader = classLoader;
        }

        // 드라이버 클래스 로드 및 인스턴스 생성
        Class<?> driverClass = classLoader.loadClass(tiberoDriverClassName);
        Driver driver = (Driver) driverClass.getDeclaredConstructor().newInstance();

        log.info("{} driver loaded successfully from: {}", version, jarFile.getAbsolutePath());
        log.info("{} driver version: {}.{}", version, driver.getMajorVersion(), driver.getMinorVersion());

        return driver;
    }

    /**
     * Tibero 클래스로더 정리
     */
    private void closeTiberoClassLoaders() {
        if (tibero6ClassLoader != null) {
            try {
                tibero6ClassLoader.close();
                log.info("Tibero6 ClassLoader closed");
            } catch (Exception e) {
                log.warn("Failed to close Tibero6 ClassLoader: {}", e.getMessage());
            }
        }
        if (tibero7ClassLoader != null) {
            try {
                tibero7ClassLoader.close();
                log.info("Tibero7 ClassLoader closed");
            } catch (Exception e) {
                log.warn("Failed to close Tibero7 ClassLoader: {}", e.getMessage());
            }
        }
    }

    // =============================================
    // Private Methods
    // =============================================

    private HikariDataSource createHikariDataSource(ExternalDatasource datasource, String decryptedPassword) {
        // Tibero는 커스텀 DataSource 생성
        if (datasource.isTibero()) {
            return createTiberoHikariDataSource(datasource, decryptedPassword);
        }

        // 다른 DB는 기본 방식
        HikariConfig config = buildHikariConfig(datasource, decryptedPassword);
        return new HikariDataSource(config);
    }

    /**
     * Tibero용 HikariDataSource 생성 (동적 드라이버 사용)
     */
    private HikariDataSource createTiberoHikariDataSource(ExternalDatasource datasource, String decryptedPassword) {
        Driver driver = getTiberoDriver(datasource.getDbType());
        if (driver == null) {
            throw new IllegalStateException("Tibero 드라이버를 로드할 수 없습니다: " + datasource.getDbType());
        }

        HikariConfig config = new HikariConfig();

        // 기본 설정 - 드라이버 클래스 대신 DataSource 직접 설정
        config.setJdbcUrl(buildJdbcUrl(datasource));
        config.setUsername(datasource.getUsername());
        config.setPassword(decryptedPassword);

        // Tibero 드라이버는 TiberoDriverDataSource를 통해 연결
        // HikariCP는 DataSource를 직접 설정하므로 driverClassName 설정 불필요

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

        // TiberoDriverDataSource 사용
        TiberoDriverDataSource tiberoDataSource = new TiberoDriverDataSource(
                driver,
                buildJdbcUrl(datasource),
                datasource.getUsername(),
                decryptedPassword
        );
        config.setDataSource(tiberoDataSource);

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

    // =============================================
    // Tibero Driver DataSource Wrapper
    // =============================================

    /**
     * Tibero Driver를 DataSource로 래핑하는 클래스
     * HikariCP가 동적 로드된 드라이버를 사용할 수 있도록 함
     */
    private static class TiberoDriverDataSource implements DataSource {
        private final Driver driver;
        private final String url;
        private final String username;
        private final String password;

        public TiberoDriverDataSource(Driver driver, String url, String username, String password) {
            this.driver = driver;
            this.url = url;
            this.username = username;
            this.password = password;
        }

        @Override
        public Connection getConnection() throws SQLException {
            return getConnection(username, password);
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            Properties props = new Properties();
            if (username != null) {
                props.setProperty("user", username);
            }
            if (password != null) {
                props.setProperty("password", password);
            }
            Connection conn = driver.connect(url, props);
            if (conn == null) {
                throw new SQLException("Driver returned null connection for URL: " + url);
            }
            return conn;
        }

        @Override
        public java.io.PrintWriter getLogWriter() throws SQLException {
            return null;
        }

        @Override
        public void setLogWriter(java.io.PrintWriter out) throws SQLException {
        }

        @Override
        public void setLoginTimeout(int seconds) throws SQLException {
        }

        @Override
        public int getLoginTimeout() throws SQLException {
            return 0;
        }

        @Override
        public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException();
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            throw new SQLException("Not a wrapper");
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            return false;
        }
    }
}
