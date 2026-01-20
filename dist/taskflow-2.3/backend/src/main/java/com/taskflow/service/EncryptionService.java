package com.taskflow.service;

/**
 * 암호화 서비스 인터페이스
 */
public interface EncryptionService {

    /**
     * 평문을 AES-256-GCM으로 암호화
     *
     * @param plainText 평문
     * @return Base64 인코딩된 암호문 (IV 포함)
     */
    String encrypt(String plainText);

    /**
     * AES-256-GCM으로 암호화된 텍스트 복호화
     *
     * @param encryptedText Base64 인코딩된 암호문
     * @return 복호화된 평문
     */
    String decrypt(String encryptedText);

    /**
     * 암호화 가능 여부 확인
     *
     * @return 암호화 서비스 사용 가능 여부
     */
    boolean isAvailable();
}
