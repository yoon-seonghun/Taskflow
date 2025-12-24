package com.taskflow.service;

import com.taskflow.domain.FileEntity;
import com.taskflow.dto.file.FileInfoResponse;
import com.taskflow.dto.file.FileUploadResponse;
import com.taskflow.exception.BusinessException;
import com.taskflow.mapper.FileMapper;
import com.taskflow.storage.FileStorageProperties;
import com.taskflow.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

/**
 * 파일 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final FileMapper fileMapper;
    private final FileStorageService fileStorageService;
    private final FileStorageProperties storageProperties;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM");

    /**
     * 이미지 MIME 타입
     */
    private static final Set<String> IMAGE_MIME_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp", "image/svg+xml"
    );

    /**
     * 파일 업로드
     *
     * @param file        업로드할 파일
     * @param relatedType 연관 엔티티 타입 (ITEM, COMMENT 등)
     * @param relatedId   연관 엔티티 ID
     * @param userId      업로드 사용자 ID
     * @return 업로드 결과
     */
    @Transactional
    public FileUploadResponse uploadFile(
            MultipartFile file,
            String relatedType,
            Long relatedId,
            Long userId
    ) throws IOException {
        // 1. 유효성 검사
        validateFile(file);

        // 2. 파일 정보 추출
        String originalName = file.getOriginalFilename();
        String extension = getExtension(originalName);
        String mimeType = file.getContentType();
        long fileSize = file.getSize();

        // 3. 저장 디렉토리 (연/월)
        String directory = LocalDate.now().format(DATE_FORMATTER);

        // 4. 스토리지에 파일 저장
        String storagePath = fileStorageService.upload(file, directory);
        String storedName = storagePath.substring(storagePath.lastIndexOf("/") + 1);

        // 5. 메타데이터 저장
        FileEntity fileEntity = new FileEntity();
        fileEntity.setOriginalName(originalName);
        fileEntity.setStoredName(storedName);
        fileEntity.setExtension(extension);
        fileEntity.setMimeType(mimeType);
        fileEntity.setFileSize(fileSize);
        fileEntity.setStorageType(fileStorageService.getStorageType().getCode());
        fileEntity.setStoragePath(storagePath);
        fileEntity.setRelatedType(relatedType);
        fileEntity.setRelatedId(relatedId);
        fileEntity.setCreatedBy(userId);

        fileMapper.insert(fileEntity);

        log.info("File uploaded: id={}, name={}, size={}, user={}",
                fileEntity.getFileId(), originalName, formatFileSize(fileSize), userId);

        // 6. 응답 생성
        String url = "/api/files/" + fileEntity.getFileId();
        return FileUploadResponse.builder()
                .fileId(fileEntity.getFileId())
                .originalName(originalName)
                .storedName(storedName)
                .extension(extension)
                .mimeType(mimeType)
                .fileSize(fileSize)
                .fileSizeDisplay(formatFileSize(fileSize))
                .storageType(fileEntity.getStorageType())
                .url(url)
                .markdown(String.format("![%s](%s)", originalName, url))
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 파일 다운로드
     *
     * @param fileId 파일 ID
     * @return 파일 리소스
     */
    public Resource downloadFile(Long fileId) throws IOException {
        FileEntity file = fileMapper.findById(fileId)
                .orElseThrow(() -> new BusinessException("파일을 찾을 수 없습니다."));

        return fileStorageService.download(file.getStoragePath());
    }

    /**
     * 파일 정보 조회
     *
     * @param fileId 파일 ID
     * @return 파일 정보
     */
    public FileInfoResponse getFileInfo(Long fileId) {
        FileEntity file = fileMapper.findById(fileId)
                .orElseThrow(() -> new BusinessException("파일을 찾을 수 없습니다."));

        return FileInfoResponse.from(file);
    }

    /**
     * 파일 엔티티 조회 (내부용)
     */
    public FileEntity getFileEntity(Long fileId) {
        return fileMapper.findById(fileId)
                .orElseThrow(() -> new BusinessException("파일을 찾을 수 없습니다."));
    }

    /**
     * 파일 삭제 (논리 삭제 + 물리 삭제)
     *
     * @param fileId 파일 ID
     * @param userId 삭제 요청 사용자 ID
     */
    @Transactional
    public void deleteFile(Long fileId, Long userId) {
        FileEntity file = fileMapper.findById(fileId)
                .orElseThrow(() -> new BusinessException("파일을 찾을 수 없습니다."));

        // 권한 확인 (본인만 삭제 가능)
        if (!file.getCreatedBy().equals(userId)) {
            throw new BusinessException("파일 삭제 권한이 없습니다.");
        }

        // 논리 삭제
        fileMapper.updateUseYn(fileId, "N", userId);

        // 물리 파일 삭제
        boolean deleted = fileStorageService.delete(file.getStoragePath());

        log.info("File deleted: id={}, name={}, physicalDeleted={}, user={}",
                fileId, file.getOriginalName(), deleted, userId);
    }

    /**
     * 연관 파일 목록 조회
     *
     * @param relatedType 연관 엔티티 타입
     * @param relatedId   연관 엔티티 ID
     * @return 파일 목록
     */
    public List<FileInfoResponse> getRelatedFiles(String relatedType, Long relatedId) {
        return fileMapper.findByRelated(relatedType, relatedId).stream()
                .map(FileInfoResponse::from)
                .toList();
    }

    /**
     * 연관 정보 업데이트
     */
    @Transactional
    public void updateRelated(Long fileId, String relatedType, Long relatedId, Long userId) {
        fileMapper.updateRelated(fileId, relatedType, relatedId, userId);
    }

    /**
     * 이미지 파일 여부 확인
     */
    public boolean isImageFile(Long fileId) {
        FileEntity file = fileMapper.findById(fileId).orElse(null);
        if (file == null) {
            return false;
        }
        return file.getMimeType() != null && IMAGE_MIME_TYPES.contains(file.getMimeType());
    }

    /**
     * 파일 유효성 검사
     */
    private void validateFile(MultipartFile file) {
        // 파일 존재 확인
        if (file == null || file.isEmpty()) {
            throw new BusinessException("파일이 비어있습니다.");
        }

        // 파일 크기 확인
        long maxSize = storageProperties.getMaxFileSizeBytes();
        if (file.getSize() > maxSize) {
            throw new BusinessException(String.format(
                    "파일 크기가 제한을 초과했습니다. (최대: %s, 현재: %s)",
                    storageProperties.getMaxFileSize(),
                    formatFileSize(file.getSize())
            ));
        }

        // 확장자 확인
        String extension = getExtension(file.getOriginalFilename());
        List<String> allowed = storageProperties.getAllowedExtensions();
        if (allowed != null && !allowed.isEmpty()) {
            if (!allowed.stream().anyMatch(ext -> ext.equalsIgnoreCase(extension))) {
                throw new BusinessException("허용되지 않는 파일 형식입니다: " + extension);
            }
        }

        // 파일명 검증 (보안)
        String filename = file.getOriginalFilename();
        if (filename != null && (filename.contains("..") || filename.contains("/"))) {
            throw new BusinessException("유효하지 않은 파일명입니다.");
        }
    }

    /**
     * 파일 확장자 추출
     */
    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 파일 크기 포맷팅
     */
    private String formatFileSize(long size) {
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
