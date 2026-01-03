package com.taskflow.security;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * MySQL sha2(password, 256) 함수와 호환되는 PasswordEncoder
 *
 * 특징:
 * - Salt 미사용 (MySQL sha2() 함수 동작과 동일)
 * - 단순 SHA-256 해싱
 * - 소문자 16진수 문자열 반환
 *
 * 사용 예:
 * MySQL: UPDATE tb_staff SET pw = sha2('password', 256) WHERE uid='user1';
 * Java: encoder.encode("password") → 동일한 해시값 생성
 */
public class Sha256PasswordEncoder implements PasswordEncoder {

    /**
     * 평문 비밀번호를 SHA-256으로 해싱
     *
     * @param rawPassword 평문 비밀번호
     * @return SHA-256 해시값 (64자리 소문자 16진수)
     */
    @Override
    public String encode(CharSequence rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("rawPassword cannot be null");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(
                rawPassword.toString().getBytes(StandardCharsets.UTF_8)
            );
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    /**
     * 평문 비밀번호와 저장된 해시값 비교
     *
     * @param rawPassword 평문 비밀번호
     * @param encodedPassword 저장된 해시값
     * @return 일치 여부
     */
    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }

        String hashedInput = encode(rawPassword);
        return hashedInput.equalsIgnoreCase(encodedPassword);
    }

    /**
     * 바이트 배열을 16진수 문자열로 변환
     *
     * @param bytes 바이트 배열
     * @return 소문자 16진수 문자열
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
