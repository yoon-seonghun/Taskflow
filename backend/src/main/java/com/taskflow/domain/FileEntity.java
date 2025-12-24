package com.taskflow.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 파일 메타데이터 엔티티
 */
@Getter
@Setter
public class FileEntity {

    /**
     * 파일 ID
     */
    private Long fileId;

    // =============================================
    // 원본 파일 정보
    // =============================================

    /**
     * 원본 파일명
     */
    private String originalName;

    /**
     * 저장된 파일명 (UUID)
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

    // =============================================
    // 스토리지 정보
    // =============================================

    /**
     * 스토리지 타입 (LOCAL, NAS, S3)
     */
    private String storageType;

    /**
     * 스토리지 내 상대 경로
     */
    private String storagePath;

    /**
     * S3 버킷명 또는 NAS 공유명
     */
    private String storageBucket;

    // =============================================
    // 연관 정보
    // =============================================

    /**
     * 연관 엔티티 타입 (ITEM, COMMENT, USER 등)
     */
    private String relatedType;

    /**
     * 연관 엔티티 ID
     */
    private Long relatedId;

    // =============================================
    // 상태
    // =============================================

    /**
     * 사용 여부 (Y/N)
     */
    private String useYn;

    // =============================================
    // 공통
    // =============================================

    /**
     * 생성일시
     */
    private LocalDateTime createdAt;

    /**
     * 생성자 ID
     */
    private Long createdBy;

    /**
     * 수정일시
     */
    private LocalDateTime updatedAt;

    /**
     * 수정자 ID
     */
    private Long updatedBy;

    // =============================================
    // 조인 필드 (조회용)
    // =============================================

    /**
     * 생성자 이름
     */
    private String createdByName;
}
