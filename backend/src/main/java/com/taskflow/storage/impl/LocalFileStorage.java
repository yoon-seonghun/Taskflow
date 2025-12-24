package com.taskflow.storage.impl;

import com.taskflow.storage.FileStorageProperties;
import com.taskflow.storage.FileStorageService;
import com.taskflow.storage.StorageType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * 로컬 파일 스토리지 구현체
 * Docker 볼륨 또는 로컬 디스크에 파일 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "file.storage.type", havingValue = "LOCAL", matchIfMissing = true)
public class LocalFileStorage implements FileStorageService {

    private final FileStorageProperties properties;
    private Path basePath;

    @PostConstruct
    public void init() {
        this.basePath = Paths.get(properties.getLocal().getBasePath())
                .toAbsolutePath()
                .normalize();

        try {
            Files.createDirectories(this.basePath);
            log.info("Local file storage initialized at: {}", this.basePath);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + this.basePath, e);
        }
    }

    @Override
    public String upload(MultipartFile file, String directory) throws IOException {
        // 저장 디렉토리 생성
        Path targetDir = basePath.resolve(directory).normalize();

        // 보안: basePath 외부로 벗어나는지 확인
        if (!targetDir.startsWith(basePath)) {
            throw new SecurityException("Invalid directory path: " + directory);
        }

        Files.createDirectories(targetDir);

        // 고유 파일명 생성 (UUID)
        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);
        String storedFilename = UUID.randomUUID().toString();
        if (!extension.isEmpty()) {
            storedFilename += "." + extension;
        }

        // 파일 저장
        Path targetPath = targetDir.resolve(storedFilename);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        // 상대 경로 반환
        String storagePath = directory + "/" + storedFilename;
        log.info("File uploaded: {} -> {}", originalFilename, storagePath);

        return storagePath;
    }

    @Override
    public Resource download(String storagePath) throws IOException {
        Path filePath = basePath.resolve(storagePath).normalize();

        // 보안: 경로 조작 방지
        if (!filePath.startsWith(basePath)) {
            throw new SecurityException("Invalid file path: " + storagePath);
        }

        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new IOException("File not found or not readable: " + storagePath);
            }
        } catch (MalformedURLException e) {
            throw new IOException("File not found: " + storagePath, e);
        }
    }

    @Override
    public boolean delete(String storagePath) {
        try {
            Path filePath = basePath.resolve(storagePath).normalize();

            // 보안: 경로 조작 방지
            if (!filePath.startsWith(basePath)) {
                log.warn("Invalid delete path attempted: {}", storagePath);
                return false;
            }

            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("File deleted: {}", storagePath);

                // 빈 디렉토리 정리 (선택적)
                cleanupEmptyDirectories(filePath.getParent());
            }
            return deleted;
        } catch (IOException e) {
            log.error("Failed to delete file: {}", storagePath, e);
            return false;
        }
    }

    @Override
    public boolean exists(String storagePath) {
        Path filePath = basePath.resolve(storagePath).normalize();
        return Files.exists(filePath) && filePath.startsWith(basePath);
    }

    @Override
    public String getAccessUrl(String storagePath) {
        // FileController를 통한 URL 반환
        return properties.getLocal().getUrlPrefix();
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.LOCAL;
    }

    @Override
    public StorageHealthStatus checkHealth() {
        long startTime = System.currentTimeMillis();

        try {
            // 디렉토리 존재 및 쓰기 권한 확인
            if (!Files.exists(basePath)) {
                return new StorageHealthStatus(
                        false,
                        StorageType.LOCAL,
                        "Base directory does not exist: " + basePath,
                        System.currentTimeMillis() - startTime
                );
            }

            if (!Files.isWritable(basePath)) {
                return new StorageHealthStatus(
                        false,
                        StorageType.LOCAL,
                        "Base directory is not writable: " + basePath,
                        System.currentTimeMillis() - startTime
                );
            }

            // 테스트 파일 쓰기/삭제
            Path testFile = basePath.resolve(".health-check-" + System.currentTimeMillis());
            Files.createFile(testFile);
            Files.delete(testFile);

            return new StorageHealthStatus(
                    true,
                    StorageType.LOCAL,
                    "Local storage is healthy. Base path: " + basePath,
                    System.currentTimeMillis() - startTime
            );

        } catch (IOException e) {
            return new StorageHealthStatus(
                    false,
                    StorageType.LOCAL,
                    "Health check failed: " + e.getMessage(),
                    System.currentTimeMillis() - startTime
            );
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
     * 빈 디렉토리 정리
     */
    private void cleanupEmptyDirectories(Path directory) {
        try {
            while (directory != null &&
                    !directory.equals(basePath) &&
                    directory.startsWith(basePath)) {

                if (Files.isDirectory(directory) && isDirectoryEmpty(directory)) {
                    Files.delete(directory);
                    log.debug("Empty directory deleted: {}", directory);
                    directory = directory.getParent();
                } else {
                    break;
                }
            }
        } catch (IOException e) {
            log.warn("Failed to cleanup empty directories: {}", e.getMessage());
        }
    }

    /**
     * 디렉토리가 비어있는지 확인
     */
    private boolean isDirectoryEmpty(Path directory) throws IOException {
        try (var entries = Files.list(directory)) {
            return entries.findFirst().isEmpty();
        }
    }
}
