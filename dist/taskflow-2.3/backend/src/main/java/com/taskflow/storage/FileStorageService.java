package com.taskflow.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 파일 스토리지 서비스 인터페이스
 * Strategy 패턴으로 다양한 스토리지 구현체 지원
 * - LocalFileStorage: 로컬 디스크/Docker 볼륨
 * - NasFileStorage: 네트워크 스토리지 (추후)
 * - S3FileStorage: AWS S3/MinIO (추후)
 */
public interface FileStorageService {

    /**
     * 파일 업로드
     *
     * @param file      업로드할 파일
     * @param directory 저장 디렉토리 (예: "2024/12")
     * @return 저장된 파일의 상대 경로
     * @throws IOException 파일 저장 실패 시
     */
    String upload(MultipartFile file, String directory) throws IOException;

    /**
     * 파일 다운로드
     *
     * @param storagePath 저장 경로 (상대 경로)
     * @return 파일 리소스
     * @throws IOException 파일 조회 실패 시
     */
    Resource download(String storagePath) throws IOException;

    /**
     * 파일 삭제
     *
     * @param storagePath 저장 경로 (상대 경로)
     * @return 삭제 성공 여부
     */
    boolean delete(String storagePath);

    /**
     * 파일 존재 여부 확인
     *
     * @param storagePath 저장 경로 (상대 경로)
     * @return 존재 여부
     */
    boolean exists(String storagePath);

    /**
     * 파일 접근 URL 반환
     *
     * @param storagePath 저장 경로 (상대 경로)
     * @return 접근 가능한 URL
     */
    String getAccessUrl(String storagePath);

    /**
     * 스토리지 타입 반환
     *
     * @return 현재 스토리지 타입
     */
    StorageType getStorageType();

    /**
     * 스토리지 연결 상태 확인
     *
     * @return 연결 상태 정보
     */
    StorageHealthStatus checkHealth();

    /**
     * 스토리지 상태 정보
     */
    record StorageHealthStatus(
            boolean healthy,
            StorageType storageType,
            String message,
            long responseTimeMs
    ) {}
}
