package com.taskflow.service.impl;

import com.taskflow.domain.SystemConfig;
import com.taskflow.dto.config.SmtpConfigResponse;
import com.taskflow.dto.config.SystemConfigUpdateRequest;
import com.taskflow.mapper.SystemConfigMapper;
import com.taskflow.service.EncryptionService;
import com.taskflow.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 시스템 설정 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigServiceImpl implements SystemConfigService {

    private final SystemConfigMapper systemConfigMapper;
    private final EncryptionService encryptionService;

    @Override
    @Transactional(readOnly = true)
    public SmtpConfigResponse getConfigsByGroup(String configGroup) {
        List<SystemConfig> configs = systemConfigMapper.selectByGroup(configGroup);
        return SmtpConfigResponse.from(configGroup, configs);
    }

    @Override
    @Transactional(readOnly = true)
    public String getConfigValue(String configGroup, String configKey) {
        return getConfigValue(configGroup, configKey, null);
    }

    @Override
    @Transactional(readOnly = true)
    public String getConfigValue(String configGroup, String configKey, String defaultValue) {
        SystemConfig config = systemConfigMapper.selectByGroupAndKey(configGroup, configKey);
        if (config == null) {
            return defaultValue;
        }

        // 암호화된 값인 경우 복호화
        if (SystemConfig.isEncryptedKey(configKey)) {
            String encryptedValue = config.getConfigValueEncrypted();
            if (encryptedValue == null || encryptedValue.isEmpty()) {
                return defaultValue;
            }
            try {
                return encryptionService.decrypt(encryptedValue);
            } catch (Exception e) {
                log.error("Failed to decrypt config value: {}.{}", configGroup, configKey, e);
                return defaultValue;
            }
        }

        String value = config.getConfigValue();
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> getSmtpConfig() {
        List<SystemConfig> configs = systemConfigMapper.selectByGroup(SystemConfig.GROUP_SMTP);
        Map<String, String> result = new HashMap<>();

        for (SystemConfig config : configs) {
            String key = config.getConfigKey();
            String value;

            if (SystemConfig.isEncryptedKey(key)) {
                // 암호화된 값 복호화
                try {
                    value = encryptionService.decrypt(config.getConfigValueEncrypted());
                } catch (Exception e) {
                    log.error("Failed to decrypt SMTP config: {}", key, e);
                    value = null;
                }
            } else {
                value = config.getConfigValue();
            }

            result.put(key, value);
        }

        return result;
    }

    @Override
    @Transactional
    public void updateConfig(String configGroup, String configKey, String value, String updatedBy) {
        if (SystemConfig.isEncryptedKey(configKey)) {
            updatePasswordConfig(configGroup, configKey, value, updatedBy);
        } else {
            systemConfigMapper.updateValue(configGroup, configKey, value, updatedBy);
        }
        log.info("Config updated: {}.{} by {}", configGroup, configKey, updatedBy);
    }

    @Override
    @Transactional
    public void updateConfigs(String configGroup, SystemConfigUpdateRequest request, String updatedBy) {
        for (SystemConfigUpdateRequest.ConfigItem item : request.getConfigs()) {
            updateConfig(configGroup, item.getKey(), item.getValue(), updatedBy);
        }
    }

    @Override
    @Transactional
    public void updatePasswordConfig(String configGroup, String configKey, String password, String updatedBy) {
        if (password == null || password.isEmpty()) {
            log.warn("Empty password provided for {}.{}", configGroup, configKey);
            return;
        }

        try {
            String encrypted = encryptionService.encrypt(password);
            systemConfigMapper.updateEncryptedValue(configGroup, configKey, encrypted, updatedBy);
            log.info("Encrypted config updated: {}.{} by {}", configGroup, configKey, updatedBy);
        } catch (Exception e) {
            log.error("Failed to encrypt password for {}.{}", configGroup, configKey, e);
            throw new RuntimeException("비밀번호 암호화에 실패했습니다", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsConfig(String configGroup, String configKey) {
        return systemConfigMapper.selectByGroupAndKey(configGroup, configKey) != null;
    }
}
