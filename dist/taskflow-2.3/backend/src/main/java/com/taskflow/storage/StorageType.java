package com.taskflow.storage;

/**
 * 스토리지 타입 열거형
 * - LOCAL: Docker 볼륨 또는 로컬 디스크
 * - SFTP: SFTP 프로토콜 기반 외부 스토리지
 * - NAS: 네트워크 스토리지 (SMB/NFS) - 추후 구현
 * - S3: AWS S3 또는 호환 스토리지 (MinIO) - 추후 구현
 */
public enum StorageType {
    LOCAL("LOCAL", "로컬 스토리지"),
    SFTP("SFTP", "SFTP 외부 스토리지"),
    NAS("NAS", "네트워크 스토리지"),
    S3("S3", "AWS S3 또는 호환 스토리지");

    private final String code;
    private final String description;

    StorageType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static StorageType fromCode(String code) {
        for (StorageType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown storage type: " + code);
    }
}
