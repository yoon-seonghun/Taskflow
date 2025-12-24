package com.taskflow.dto.file;

import com.taskflow.domain.FileEntity;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 파일 정보 응답 DTO
 */
@Getter
@Builder
public class FileInfoResponse {

    /**
     * 파일 ID
     */
    private Long fileId;

    /**
     * 원본 파일명
     */
    private String originalName;

    /**
     * 저장된 파일명
     */
    private String storedName;

    /**
     * 파일 확장자
     */
    private String extension;

    /**
     * MIME 타입
     */
    private String mimeType;

    /**
     * 파일 크기 (bytes)
     */
    private Long fileSize;

    /**
     * 파일 크기 (표시용)
     */
    private String fileSizeDisplay;

    /**
     * 스토리지 타입
     */
    private String storageType;

    /**
     * 연관 엔티티 타입
     */
    private String relatedType;

    /**
     * 연관 엔티티 ID
     */
    private Long relatedId;

    /**
     * 파일 접근 URL
     */
    private String url;

    /**
     * 생성일시
     */
    private LocalDateTime createdAt;

    /**
     * 생성자 ID
     */
    private Long createdBy;

    /**
     * 생성자 이름
     */
    private String createdByName;

    /**
     * FileEntity에서 변환
     */
    public static FileInfoResponse from(FileEntity entity) {
        return FileInfoResponse.builder()
                .fileId(entity.getFileId())
                .originalName(entity.getOriginalName())
                .storedName(entity.getStoredName())
                .extension(entity.getExtension())
                .mimeType(entity.getMimeType())
                .fileSize(entity.getFileSize())
                .fileSizeDisplay(formatFileSize(entity.getFileSize()))
                .storageType(entity.getStorageType())
                .relatedType(entity.getRelatedType())
                .relatedId(entity.getRelatedId())
                .url("/api/files/" + entity.getFileId())
                .createdAt(entity.getCreatedAt())
                .createdBy(entity.getCreatedBy())
                .createdByName(entity.getCreatedByName())
                .build();
    }

    /**
     * 파일 크기 포맷팅
     */
    private static String formatFileSize(Long size) {
        if (size == null || size == 0) {
            return "0 B";
        }
        if (size < 1024) {
            return size + " B";
        }
        if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        }
        if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024));
        }
        return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
    }
}
