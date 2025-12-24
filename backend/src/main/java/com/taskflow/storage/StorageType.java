package com.taskflow.storage;

/**
 * 스토리지 타입 열거형
 * - LOCAL: Docker 볼륨 또는 로컬 디스크
 * - NAS: 네트워크 스토리지 (SMB/NFS)
 * - S3: AWS S3 또는 호환 스토리지 (MinIO)
 */
public enum StorageType {
    LOCAL("LOCAL", "로컬 스토리지"),
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
