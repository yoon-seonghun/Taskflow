package com.taskflow.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 사용자 관리 설정 프로퍼티
 *
 * 두 가지 모드 지원:
 * - internal: 기본 관리 모드 (BCrypt, CRUD 가능)
 * - external: 외부 DB 연동 모드 (SHA256, 조회만 가능)
 */
@Slf4j
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "taskflow.user-management")
public class UserManagementProperties {

    /**
     * 사용자 관리 모드
     * - internal: 기본 관리 모드 (TaskFlow DB 사용)
     * - external: 외부 DB 연동 모드 (외부 DB VIEW 사용)
     */
    private String mode = "internal";

    /**
     * Internal 모드 설정
     */
    private InternalConfig internal = new InternalConfig();

    /**
     * External 모드 설정
     */
    private ExternalConfig external = new ExternalConfig();

    /**
     * Internal 모드 여부 확인
     */
    public boolean isInternalMode() {
        return "internal".equalsIgnoreCase(mode);
    }

    /**
     * External 모드 여부 확인
     */
    public boolean isExternalMode() {
        return "external".equalsIgnoreCase(mode);
    }

    /**
     * 사용자 CRUD 활성화 여부
     */
    public boolean isUserCrudEnabled() {
        if (isExternalMode()) {
            return external.isUserCrudEnabled();
        }
        return internal.isUserCrudEnabled();
    }

    /**
     * 부서 CRUD 활성화 여부
     */
    public boolean isDepartmentCrudEnabled() {
        if (isExternalMode()) {
            return external.isDepartmentCrudEnabled();
        }
        return internal.isDepartmentCrudEnabled();
    }

    /**
     * 직급 CRUD 활성화 여부
     */
    public boolean isPositionCrudEnabled() {
        if (isExternalMode()) {
            return external.isPositionCrudEnabled();
        }
        return internal.isPositionCrudEnabled();
    }

    /**
     * Internal 모드 설정 클래스
     */
    @Getter
    @Setter
    public static class InternalConfig {
        /**
         * 패스워드 인코더 타입: bcrypt
         */
        private String passwordEncoder = "bcrypt";

        /**
         * BCrypt strength (4~31, 기본값: 10)
         */
        private int bcryptStrength = 10;

        /**
         * 사용자 CRUD 활성화 여부
         */
        private boolean userCrudEnabled = true;

        /**
         * 부서 CRUD 활성화 여부
         */
        private boolean departmentCrudEnabled = true;

        /**
         * 직급 CRUD 활성화 여부
         */
        private boolean positionCrudEnabled = true;
    }

    /**
     * External 모드 설정 클래스
     */
    @Getter
    @Setter
    public static class ExternalConfig {

        /**
         * 테이블/뷰 이름 검증 패턴
         * - 영문자, 숫자, 언더스코어만 허용
         * - 스키마.테이블 형식 허용 (예: schema.VW_USER)
         * - SQL Injection 방지
         */
        private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)?$");

        /**
         * 패스워드 인코더 타입: sha256
         */
        private String passwordEncoder = "sha256";

        /**
         * SHA256 Salt 사용 여부 (MySQL sha2() 함수는 Salt 미사용)
         */
        private boolean sha256UseSalt = false;

        /**
         * 사용자 CRUD 활성화 여부 (외부 모드에서는 기본 false)
         */
        private boolean userCrudEnabled = false;

        /**
         * 부서 CRUD 활성화 여부 (외부 모드에서는 기본 false)
         */
        private boolean departmentCrudEnabled = false;

        /**
         * 직급 CRUD 활성화 여부 (외부 모드에서는 기본 false)
         */
        private boolean positionCrudEnabled = false;

        /**
         * 외부 DB 사용자 테이블/뷰 이름
         */
        private String userTable = "VW_TASKFLOW_USER";

        /**
         * 외부 DB 부서 테이블/뷰 이름
         */
        private String departmentTable = "VW_TASKFLOW_DEPARTMENT";

        /**
         * 외부 DB 직급 테이블/뷰 이름
         */
        private String positionTable = "VW_TASKFLOW_POSITION";

        /**
         * 테이블/뷰 이름 유효성 검증
         * @param tableName 검증할 테이블 이름
         * @return 유효한 테이블 이름인 경우 true
         */
        public static boolean isValidTableName(String tableName) {
            if (tableName == null || tableName.isBlank()) {
                return false;
            }
            // 최대 길이 제한 (128자)
            if (tableName.length() > 128) {
                return false;
            }
            return TABLE_NAME_PATTERN.matcher(tableName).matches();
        }

        /**
         * 사용자 테이블 설정 시 유효성 검증
         */
        public void setUserTable(String userTable) {
            if (!isValidTableName(userTable)) {
                throw new IllegalArgumentException(
                    "Invalid user table name: " + userTable +
                    ". Only alphanumeric characters and underscores are allowed."
                );
            }
            this.userTable = userTable;
        }

        /**
         * 부서 테이블 설정 시 유효성 검증
         */
        public void setDepartmentTable(String departmentTable) {
            if (!isValidTableName(departmentTable)) {
                throw new IllegalArgumentException(
                    "Invalid department table name: " + departmentTable +
                    ". Only alphanumeric characters and underscores are allowed."
                );
            }
            this.departmentTable = departmentTable;
        }

        /**
         * 직급 테이블 설정 시 유효성 검증
         */
        public void setPositionTable(String positionTable) {
            if (!isValidTableName(positionTable)) {
                throw new IllegalArgumentException(
                    "Invalid position table name: " + positionTable +
                    ". Only alphanumeric characters and underscores are allowed."
                );
            }
            this.positionTable = positionTable;
        }

        /**
         * 설정된 테이블 이름들의 유효성 검증
         * @throws IllegalStateException 잘못된 테이블 이름이 있는 경우
         */
        public void validateTableNames() {
            if (!isValidTableName(userTable)) {
                throw new IllegalStateException(
                    "Invalid user table name configured: " + userTable
                );
            }
            if (!isValidTableName(departmentTable)) {
                throw new IllegalStateException(
                    "Invalid department table name configured: " + departmentTable
                );
            }
            if (!isValidTableName(positionTable)) {
                throw new IllegalStateException(
                    "Invalid position table name configured: " + positionTable
                );
            }
        }
    }

    /**
     * 설정 로드 후 유효성 검증
     */
    @PostConstruct
    public void validateConfiguration() {
        log.info("User management mode: {}", mode);

        if (isExternalMode()) {
            log.info("External mode enabled - validating table names...");
            external.validateTableNames();
            log.info("External DB tables - User: {}, Department: {}, Position: {}",
                external.getUserTable(), external.getDepartmentTable(), external.getPositionTable());
        } else {
            log.info("Internal mode enabled - using TaskFlow database");
        }
    }
}
