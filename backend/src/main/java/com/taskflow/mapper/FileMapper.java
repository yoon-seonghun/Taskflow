package com.taskflow.mapper;

import com.taskflow.domain.FileEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 파일 메타데이터 Mapper
 */
@Mapper
public interface FileMapper {

    /**
     * 파일 메타데이터 저장
     */
    void insert(FileEntity file);

    /**
     * 파일 ID로 조회
     */
    Optional<FileEntity> findById(Long fileId);

    /**
     * 연관 엔티티로 파일 목록 조회
     */
    List<FileEntity> findByRelated(
            @Param("relatedType") String relatedType,
            @Param("relatedId") Long relatedId
    );

    /**
     * 사용자가 업로드한 파일 목록 조회
     */
    List<FileEntity> findByCreatedBy(
            @Param("createdBy") Long createdBy,
            @Param("limit") int limit
    );

    /**
     * 사용 여부 업데이트 (논리 삭제)
     */
    void updateUseYn(
            @Param("fileId") Long fileId,
            @Param("useYn") String useYn,
            @Param("updatedBy") Long updatedBy
    );

    /**
     * 연관 정보 업데이트
     */
    void updateRelated(
            @Param("fileId") Long fileId,
            @Param("relatedType") String relatedType,
            @Param("relatedId") Long relatedId,
            @Param("updatedBy") Long updatedBy
    );

    /**
     * 스토리지 정보 업데이트 (마이그레이션용)
     */
    void updateStorage(
            @Param("fileId") Long fileId,
            @Param("storageType") String storageType,
            @Param("storagePath") String storagePath,
            @Param("storageBucket") String storageBucket,
            @Param("updatedBy") Long updatedBy
    );

    /**
     * 물리적 삭제
     */
    void delete(Long fileId);

    /**
     * 스토리지 타입별 파일 수 조회
     */
    int countByStorageType(@Param("storageType") String storageType);

    /**
     * 미사용 파일 목록 조회 (정리용)
     */
    List<FileEntity> findUnusedFiles(@Param("limit") int limit);
}
