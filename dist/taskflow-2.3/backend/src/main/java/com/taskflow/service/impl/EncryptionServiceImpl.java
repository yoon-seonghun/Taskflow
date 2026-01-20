package com.taskflow.service.impl;

import com.taskflow.config.EncryptionConfig;
import com.taskflow.service.EncryptionService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM 암호화 서비스 구현
 *
 * 특징:
 * - AES-256-GCM (Galois/Counter Mode) 사용
 * - 암호문에 IV(12바이트) 포함
 * - 인증 태그 길이: 128비트
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EncryptionServiceImpl implements EncryptionService {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits (권장)
    private static final int GCM_TAG_LENGTH = 128; // 128 bits

    private final EncryptionConfig encryptionConfig;

    private SecretKey secretKey;
    private boolean available = false;

    @PostConstruct
    public void init() {
        try {
            String base64Key = encryptionConfig.getSecretKey();
            if (base64Key == null || base64Key.isBlank()) {
                log.warn("AES secret key is not configured. Encryption service will be unavailable.");
                return;
            }

            byte[] keyBytes = Base64.getDecoder().decode(base64Key);

            // AES-256은 32바이트 키 필요
            if (keyBytes.length < 32) {
                log.warn("AES secret key must be at least 32 bytes for AES-256. Current: {} bytes", keyBytes.length);
                // 키가 짧으면 패딩
                byte[] paddedKey = new byte[32];
                System.arraycopy(keyBytes, 0, paddedKey, 0, Math.min(keyBytes.length, 32));
                keyBytes = paddedKey;
            } else if (keyBytes.length > 32) {
                // 키가 길면 자름
                byte[] trimmedKey = new byte[32];
                System.arraycopy(keyBytes, 0, trimmedKey, 0, 32);
                keyBytes = trimmedKey;
            }

            secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
            available = true;
            log.info("AES-256-GCM encryption service initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize encryption service: {}", e.getMessage());
            available = false;
        }
    }

    @Override
    public String encrypt(String plainText) {
        if (!available) {
            throw new IllegalStateException("Encryption service is not available");
        }

        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }

        try {
            // IV 생성 (랜덤)
            byte[] iv = new byte[GCM_IV_LENGTH];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            // Cipher 초기화
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            // 암호화
            byte[] encrypted = cipher.doFinal(plainText.getBytes());

            // IV + 암호문 결합
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encrypted.length);
            byteBuffer.put(iv);
            byteBuffer.put(encrypted);

            // Base64 인코딩
            return Base64.getEncoder().encodeToString(byteBuffer.array());

        } catch (Exception e) {
            log.error("Encryption failed: {}", e.getMessage());
            throw new RuntimeException("Encryption failed", e);
        }
    }

    @Override
    public String decrypt(String encryptedText) {
        if (!available) {
            throw new IllegalStateException("Encryption service is not available");
        }

        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }

        try {
            // Base64 디코딩
            byte[] decoded = Base64.getDecoder().decode(encryptedText);

            // IV와 암호문 분리
            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);
            byte[] encrypted = new byte[byteBuffer.remaining()];
            byteBuffer.get(encrypted);

            // Cipher 초기화
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            // 복호화
            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted);

        } catch (Exception e) {
            log.error("Decryption failed: {}", e.getMessage());
            throw new RuntimeException("Decryption failed", e);
        }
    }

    @Override
    public boolean isAvailable() {
        return available;
    }
}
