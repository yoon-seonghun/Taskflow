package com.taskflow.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 시스템 설정 엔티티
 *
 * 테이블: TB_SYSTEM_CONFIG
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemConfig {

    /**
     * 설정 ID (PK)
     */
    private Long configId;

    /**
     * 설정 그룹 (SMTP, SYSTEM 등)
     */
    private String configGroup;

    /**
     * 설정 키
     */
    private String configKey;

    /**
     * 설정 값
     */
    private String configValue;

    /**
     * 암호화된 설정 값 (비밀번호 등)
     */
    private String configValueEncrypted;

    /**
     * 설정 설명
     */
    private String description;

    /**
     * 사용 여부 (Y/N)
     */
    private String useYn;

    /**
     * 생성일시
     */
    private LocalDateTime createdAt;

    /**
     * 생성자 USERNAME
     */
    private String createdBy;

    /**
     * 수정일시
     */
    private LocalDateTime updatedAt;

    /**
     * 수정자 USERNAME
     */
    private String updatedBy;

    // =============================================
    // 상수: 설정 그룹
    // =============================================
    public static final String GROUP_SMTP = "SMTP";
    public static final String GROUP_SYSTEM = "SYSTEM";

    // =============================================
    // 상수: SMTP 설정 키
    // =============================================
    public static final String SMTP_HOST = "HOST";
    public static final String SMTP_PORT = "PORT";
    public static final String SMTP_SECURITY_TYPE = "SECURITY_TYPE";
    public static final String SMTP_USERNAME = "USERNAME";
    public static final String SMTP_PASSWORD = "PASSWORD";
    public static final String SMTP_FROM_ADDRESS = "FROM_ADDRESS";
    public static final String SMTP_ADMIN_EMAIL = "ADMIN_EMAIL";

    // =============================================
    // 상수: 보안 유형
    // =============================================
    public static final String SECURITY_STARTTLS = "STARTTLS";
    public static final String SECURITY_TLS = "TLS";
    public static final String SECURITY_NONE = "NONE";

    /**
     * 암호화가 필요한 키인지 확인
     */
    public static boolean isEncryptedKey(String key) {
        return SMTP_PASSWORD.equals(key);
    }

    /**
     * 실제 값 반환 (암호화된 값 또는 일반 값)
     */
    public String getEffectiveValue() {
        if (configValueEncrypted != null && !configValueEncrypted.isEmpty()) {
            return configValueEncrypted;
        }
        return configValue;
    }
}
