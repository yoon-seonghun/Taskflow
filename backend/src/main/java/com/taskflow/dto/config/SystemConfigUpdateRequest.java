package com.taskflow.dto.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * 시스템 설정 수정 요청 DTO
 */
@Getter
@Setter
public class SystemConfigUpdateRequest {

    /**
     * 설정 항목 목록
     */
    @NotEmpty(message = "설정 항목이 필요합니다")
    @Valid
    private List<ConfigItem> configs;

    /**
     * 설정 항목
     */
    @Getter
    @Setter
    public static class ConfigItem {
        @NotBlank(message = "설정 키는 필수입니다")
        private String key;

        private String value;
    }
}
