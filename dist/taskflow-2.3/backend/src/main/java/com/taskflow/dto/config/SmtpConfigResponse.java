package com.taskflow.dto.config;

import com.taskflow.domain.SystemConfig;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

/**
 * SMTP 설정 응답 DTO
 */
@Getter
@Builder
public class SmtpConfigResponse {

    /**
     * 설정 그룹
     */
    private String group;

    /**
     * 설정 항목 목록
     */
    private List<ConfigItem> configs;

    /**
     * 설정 항목
     */
    @Getter
    @Builder
    public static class ConfigItem {
        private String key;
        private String value;
        private String description;
        private boolean encrypted;
    }

    /**
     * 도메인 리스트를 응답 DTO로 변환
     */
    public static SmtpConfigResponse from(String group, List<SystemConfig> configs) {
        List<ConfigItem> items = configs.stream()
                .map(config -> ConfigItem.builder()
                        .key(config.getConfigKey())
                        .value(maskSensitiveValue(config))
                        .description(config.getDescription())
                        .encrypted(SystemConfig.isEncryptedKey(config.getConfigKey()))
                        .build())
                .collect(Collectors.toList());

        return SmtpConfigResponse.builder()
                .group(group)
                .configs(items)
                .build();
    }

    /**
     * 민감한 값 마스킹 처리
     */
    private static String maskSensitiveValue(SystemConfig config) {
        if (SystemConfig.isEncryptedKey(config.getConfigKey())) {
            // 비밀번호 등 암호화 키는 마스킹
            String value = config.getConfigValueEncrypted();
            if (value != null && !value.isEmpty()) {
                return "********";
            }
            return "";
        }
        return config.getConfigValue();
    }
}
