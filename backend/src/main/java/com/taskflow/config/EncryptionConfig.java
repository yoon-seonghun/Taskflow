package com.taskflow.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AES 암호화 설정
 */
@Configuration
@ConfigurationProperties(prefix = "encryption.aes")
@Getter
@Setter
public class EncryptionConfig {

    /**
     * AES-256 암호화 키 (Base64 인코딩)
     * 최소 32바이트 (256비트) 필요
     */
    private String secretKey;
}
