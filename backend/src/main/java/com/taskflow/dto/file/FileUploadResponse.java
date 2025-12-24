package com.taskflow.dto.file;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 파일 업로드 응답 DTO
 */
@Getter
@Builder
public class FileUploadResponse {

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
     * 파일 접근 URL
     */
    private String url;

    /**
     * 마크다운 이미지 문법
     */
    private String markdown;

    /**
     * 생성일시
     */
    private LocalDateTime createdAt;
}
