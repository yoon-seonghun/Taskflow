package com.taskflow.service;

import com.taskflow.domain.SystemConfig;
import com.taskflow.dto.config.SmtpConfigResponse;
import com.taskflow.dto.config.SystemConfigUpdateRequest;

import java.util.List;
import java.util.Map;

/**
 * 시스템 설정 서비스 인터페이스
 */
public interface SystemConfigService {

    /**
     * 설정 그룹별 목록 조회
     */
    SmtpConfigResponse getConfigsByGroup(String configGroup);

    /**
     * 특정 설정 값 조회 (복호화된 값)
     */
    String getConfigValue(String configGroup, String configKey);

    /**
     * 특정 설정 값 조회 (기본값 지정)
     */
    String getConfigValue(String configGroup, String configKey, String defaultValue);

    /**
     * SMTP 설정 조회
     */
    Map<String, String> getSmtpConfig();

    /**
     * 설정 값 업데이트
     */
    void updateConfig(String configGroup, String configKey, String value, String updatedBy);

    /**
     * 여러 설정 일괄 업데이트
     */
    void updateConfigs(String configGroup, SystemConfigUpdateRequest request, String updatedBy);

    /**
     * 비밀번호 설정 업데이트 (암호화)
     */
    void updatePasswordConfig(String configGroup, String configKey, String password, String updatedBy);

    /**
     * 설정 존재 여부 확인
     */
    boolean existsConfig(String configGroup, String configKey);
}
