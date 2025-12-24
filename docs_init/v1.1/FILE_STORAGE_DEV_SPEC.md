# 파일 스토리지 시스템 개발 지시서

| 항목 | 내용 |
|------|------|
| 문서 버전 | 1.0 |
| 작성일 | 2024-12-23 |
| 대상 버전 | TaskFlow v1.1 |
| 관련 설계서 | FILE_STORAGE_DESIGN.md |

---

## 1. 개발 개요

### 1.1 개발 목표
마크다운 에디터에서 이미지 첨부 기능 구현 (드래그앤드롭, 클립보드, 버튼 클릭)

### 1.2 개발 범위

| Phase | 내용 | 우선순위 |
|-------|------|---------|
| Phase 1 | 백엔드 - 스토리지 인프라 | 필수 |
| Phase 2 | 백엔드 - 파일 API | 필수 |
| Phase 3 | 프론트엔드 - 이미지 업로드 UI | 필수 |
| Phase 4 | NAS/S3 스토리지 확장 | 추후 |

---

## 2. 개발 환경

### 2.1 기술 스택

| 구분 | 기술 |
|------|------|
| Backend | Spring Boot 3.x, Java 17 |
| ORM | MyBatis (XML Mapper) |
| Database | MySQL 8.0 |
| Frontend | Vue 3, TypeScript |
| Storage | Docker Volume (LOCAL) |

### 2.2 개발 브랜치

```
feature/file-storage
```

---

## 3. 백엔드 개발 지시

### 3.1 파일 구조

```
backend/src/main/java/com/taskflow/
├── storage/
│   ├── FileStorageService.java           [신규]
│   ├── FileStorageProperties.java        [신규]
│   ├── StorageType.java                  [신규]
│   ├── FileStorageFactory.java           [신규]
│   └── impl/
│       └── LocalFileStorage.java         [신규]
├── controller/
│   └── FileController.java               [신규]
├── service/
│   └── FileService.java                  [신규]
├── mapper/
│   └── FileMapper.java                   [신규]
├── domain/
│   └── FileEntity.java                   [신규]
└── dto/
    └── file/
        ├── FileUploadRequest.java        [신규]
        ├── FileUploadResponse.java       [신규]
        └── FileInfoResponse.java         [신규]

backend/src/main/resources/
├── mapper/
│   └── FileMapper.xml                    [신규]
└── application.yml                       [수정]
```

---

### 3.2 구현 상세

#### 3.2.1 StorageType.java

```java
package com.taskflow.storage;

/**
 * 스토리지 타입 열거형
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
}
```

#### 3.2.2 FileStorageProperties.java

```java
package com.taskflow.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

/**
 * 파일 스토리지 설정
 */
@Component
@ConfigurationProperties(prefix = "storage")
@Getter
@Setter
public class FileStorageProperties {

    private StorageType type = StorageType.LOCAL;
    private String maxFileSize = "10MB";
    private List<String> allowedExtensions;

    private LocalStorage local = new LocalStorage();
    private NasStorage nas = new NasStorage();
    private S3Storage s3 = new S3Storage();

    @Getter
    @Setter
    public static class LocalStorage {
        private String basePath = "/data/uploads";
        private String urlPrefix = "/api/files";
    }

    @Getter
    @Setter
    public static class NasStorage {
        private String basePath;
        private String shareName;
        private String urlPrefix = "/api/files";
    }

    @Getter
    @Setter
    public static class S3Storage {
        private String endpoint;
        private String bucket;
        private String region;
        private String accessKey;
        private String secretKey;
        private String urlPrefix;
    }
}
```

#### 3.2.3 FileStorageService.java (인터페이스)

```java
package com.taskflow.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

/**
 * 파일 스토리지 서비스 인터페이스
 * - Strategy 패턴으로 다양한 스토리지 구현체 지원
 */
public interface FileStorageService {

    /**
     * 파일 업로드
     * @param file 업로드할 파일
     * @param directory 저장 디렉토리 (예: "2024/12")
     * @return 저장된 파일의 상대 경로
     */
    String upload(MultipartFile file, String directory) throws IOException;

    /**
     * 파일 다운로드
     * @param storagePath 저장 경로
     * @return 파일 리소스
     */
    Resource download(String storagePath) throws IOException;

    /**
     * 파일 삭제
     * @param storagePath 저장 경로
     * @return 삭제 성공 여부
     */
    boolean delete(String storagePath);

    /**
     * 파일 존재 여부 확인
     * @param storagePath 저장 경로
     * @return 존재 여부
     */
    boolean exists(String storagePath);

    /**
     * 파일 접근 URL 반환
     * @param storagePath 저장 경로
     * @return 접근 URL
     */
    String getAccessUrl(String storagePath);

    /**
     * 스토리지 타입 반환
     */
    StorageType getStorageType();
}
```

#### 3.2.4 LocalFileStorage.java

```java
package com.taskflow.storage.impl;

import com.taskflow.storage.FileStorageProperties;
import com.taskflow.storage.FileStorageService;
import com.taskflow.storage.StorageType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * 로컬 파일 스토리지 구현체
 * - Docker 볼륨 또는 로컬 디스크에 파일 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.type", havingValue = "LOCAL", matchIfMissing = true)
public class LocalFileStorage implements FileStorageService {

    private final FileStorageProperties properties;
    private Path basePath;

    @PostConstruct
    public void init() {
        this.basePath = Paths.get(properties.getLocal().getBasePath()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.basePath);
            log.info("Local file storage initialized at: {}", this.basePath);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    @Override
    public String upload(MultipartFile file, String directory) throws IOException {
        // 저장 디렉토리 생성
        Path targetDir = basePath.resolve(directory).normalize();
        Files.createDirectories(targetDir);

        // 고유 파일명 생성
        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);
        String storedFilename = UUID.randomUUID().toString() + "." + extension;

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

        // 경로 조작 방지
        if (!filePath.startsWith(basePath)) {
            throw new SecurityException("Invalid file path");
        }

        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new IOException("File not found: " + storagePath);
            }
        } catch (MalformedURLException e) {
            throw new IOException("File not found: " + storagePath, e);
        }
    }

    @Override
    public boolean delete(String storagePath) {
        try {
            Path filePath = basePath.resolve(storagePath).normalize();

            // 경로 조작 방지
            if (!filePath.startsWith(basePath)) {
                log.warn("Invalid delete path attempted: {}", storagePath);
                return false;
            }

            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("File deleted: {}", storagePath);
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
        // 파일 ID 기반 URL 사용 (FileController에서 처리)
        return properties.getLocal().getUrlPrefix();
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.LOCAL;
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}
```

#### 3.2.5 FileEntity.java

```java
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

    private Long fileId;

    // 원본 파일 정보
    private String originalName;
    private String storedName;
    private String extension;
    private String mimeType;
    private Long fileSize;

    // 스토리지 정보
    private String storageType;
    private String storagePath;
    private String storageBucket;

    // 연관 정보
    private String relatedType;
    private Long relatedId;

    // 상태
    private String useYn;

    // 공통
    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;

    // 조인 필드
    private String createdByName;
}
```

#### 3.2.6 FileMapper.java

```java
package com.taskflow.mapper;

import com.taskflow.domain.FileEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Optional;

@Mapper
public interface FileMapper {

    void insert(FileEntity file);

    Optional<FileEntity> findById(Long fileId);

    List<FileEntity> findByRelated(
        @Param("relatedType") String relatedType,
        @Param("relatedId") Long relatedId
    );

    void updateUseYn(
        @Param("fileId") Long fileId,
        @Param("useYn") String useYn,
        @Param("updatedBy") Long updatedBy
    );

    void delete(Long fileId);
}
```

#### 3.2.7 FileMapper.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.taskflow.mapper.FileMapper">

    <resultMap id="FileResultMap" type="com.taskflow.domain.FileEntity">
        <id property="fileId" column="FILE_ID"/>
        <result property="originalName" column="ORIGINAL_NAME"/>
        <result property="storedName" column="STORED_NAME"/>
        <result property="extension" column="EXTENSION"/>
        <result property="mimeType" column="MIME_TYPE"/>
        <result property="fileSize" column="FILE_SIZE"/>
        <result property="storageType" column="STORAGE_TYPE"/>
        <result property="storagePath" column="STORAGE_PATH"/>
        <result property="storageBucket" column="STORAGE_BUCKET"/>
        <result property="relatedType" column="RELATED_TYPE"/>
        <result property="relatedId" column="RELATED_ID"/>
        <result property="useYn" column="USE_YN"/>
        <result property="createdAt" column="CREATED_AT"/>
        <result property="createdBy" column="CREATED_BY"/>
        <result property="updatedAt" column="UPDATED_AT"/>
        <result property="updatedBy" column="UPDATED_BY"/>
        <result property="createdByName" column="CREATED_BY_NAME"/>
    </resultMap>

    <insert id="insert" useGeneratedKeys="true" keyProperty="fileId" keyColumn="FILE_ID">
        INSERT INTO TB_FILE (
            ORIGINAL_NAME, STORED_NAME, EXTENSION, MIME_TYPE, FILE_SIZE,
            STORAGE_TYPE, STORAGE_PATH, STORAGE_BUCKET,
            RELATED_TYPE, RELATED_ID,
            USE_YN, CREATED_BY
        ) VALUES (
            #{originalName}, #{storedName}, #{extension}, #{mimeType}, #{fileSize},
            #{storageType}, #{storagePath}, #{storageBucket},
            #{relatedType}, #{relatedId},
            'Y', #{createdBy}
        )
    </insert>

    <select id="findById" resultMap="FileResultMap">
        SELECT f.*,
               u.NAME as CREATED_BY_NAME
        FROM TB_FILE f
        LEFT JOIN TB_USER u ON f.CREATED_BY = u.USER_ID
        WHERE f.FILE_ID = #{fileId}
          AND f.USE_YN = 'Y'
    </select>

    <select id="findByRelated" resultMap="FileResultMap">
        SELECT f.*,
               u.NAME as CREATED_BY_NAME
        FROM TB_FILE f
        LEFT JOIN TB_USER u ON f.CREATED_BY = u.USER_ID
        WHERE f.RELATED_TYPE = #{relatedType}
          AND f.RELATED_ID = #{relatedId}
          AND f.USE_YN = 'Y'
        ORDER BY f.CREATED_AT DESC
    </select>

    <update id="updateUseYn">
        UPDATE TB_FILE
        SET USE_YN = #{useYn},
            UPDATED_BY = #{updatedBy}
        WHERE FILE_ID = #{fileId}
    </update>

    <delete id="delete">
        DELETE FROM TB_FILE WHERE FILE_ID = #{fileId}
    </delete>

</mapper>
```

#### 3.2.8 FileService.java

```java
package com.taskflow.service;

import com.taskflow.domain.FileEntity;
import com.taskflow.dto.file.FileUploadResponse;
import com.taskflow.dto.file.FileInfoResponse;
import com.taskflow.exception.BusinessException;
import com.taskflow.mapper.FileMapper;
import com.taskflow.storage.FileStorageService;
import com.taskflow.storage.FileStorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final FileMapper fileMapper;
    private final FileStorageService fileStorageService;
    private final FileStorageProperties storageProperties;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM");

    /**
     * 파일 업로드
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

        log.info("File uploaded: id={}, name={}, size={}",
                fileEntity.getFileId(), originalName, fileSize);

        // 6. 응답 생성
        return FileUploadResponse.builder()
                .fileId(fileEntity.getFileId())
                .originalName(originalName)
                .storedName(storedName)
                .extension(extension)
                .mimeType(mimeType)
                .fileSize(fileSize)
                .storageType(fileEntity.getStorageType())
                .url("/api/files/" + fileEntity.getFileId())
                .markdown(String.format("![%s](/api/files/%d)", originalName, fileEntity.getFileId()))
                .build();
    }

    /**
     * 파일 다운로드
     */
    public Resource downloadFile(Long fileId) throws IOException {
        FileEntity file = fileMapper.findById(fileId)
                .orElseThrow(() -> new BusinessException("파일을 찾을 수 없습니다."));

        return fileStorageService.download(file.getStoragePath());
    }

    /**
     * 파일 정보 조회
     */
    public FileInfoResponse getFileInfo(Long fileId) {
        FileEntity file = fileMapper.findById(fileId)
                .orElseThrow(() -> new BusinessException("파일을 찾을 수 없습니다."));

        return FileInfoResponse.from(file);
    }

    /**
     * 파일 삭제 (논리 삭제)
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
        fileStorageService.delete(file.getStoragePath());

        log.info("File deleted: id={}, name={}", fileId, file.getOriginalName());
    }

    /**
     * 연관 파일 목록 조회
     */
    public List<FileInfoResponse> getRelatedFiles(String relatedType, Long relatedId) {
        return fileMapper.findByRelated(relatedType, relatedId).stream()
                .map(FileInfoResponse::from)
                .toList();
    }

    /**
     * 파일 유효성 검사
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("파일이 비어있습니다.");
        }

        // 확장자 검사
        String extension = getExtension(file.getOriginalFilename());
        List<String> allowed = storageProperties.getAllowedExtensions();
        if (allowed != null && !allowed.isEmpty() && !allowed.contains(extension.toLowerCase())) {
            throw new BusinessException("허용되지 않는 파일 형식입니다: " + extension);
        }

        // 파일 크기 검사
        long maxSize = parseSize(storageProperties.getMaxFileSize());
        if (file.getSize() > maxSize) {
            throw new BusinessException("파일 크기가 제한을 초과했습니다. (최대: " + storageProperties.getMaxFileSize() + ")");
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    private long parseSize(String size) {
        size = size.toUpperCase();
        if (size.endsWith("KB")) {
            return Long.parseLong(size.replace("KB", "").trim()) * 1024;
        } else if (size.endsWith("MB")) {
            return Long.parseLong(size.replace("MB", "").trim()) * 1024 * 1024;
        } else if (size.endsWith("GB")) {
            return Long.parseLong(size.replace("GB", "").trim()) * 1024 * 1024 * 1024;
        }
        return Long.parseLong(size);
    }
}
```

#### 3.2.9 FileController.java

```java
package com.taskflow.controller;

import com.taskflow.common.ApiResponse;
import com.taskflow.dto.file.FileUploadResponse;
import com.taskflow.dto.file.FileInfoResponse;
import com.taskflow.security.SecurityUtils;
import com.taskflow.service.FileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 파일 관리 API
 *
 * - POST   /api/files/upload           파일 업로드
 * - GET    /api/files/{fileId}         파일 다운로드/표시
 * - GET    /api/files/{fileId}/info    파일 정보 조회
 * - DELETE /api/files/{fileId}         파일 삭제
 * - GET    /api/files/related/{type}/{id}  연관 파일 목록
 */
@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /**
     * 파일 업로드
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "relatedType", required = false) String relatedType,
            @RequestParam(value = "relatedId", required = false) Long relatedId
    ) throws IOException {
        Long userId = SecurityUtils.getCurrentUserId();

        FileUploadResponse response = fileService.uploadFile(file, relatedType, relatedId, userId);

        return ResponseEntity.ok(ApiResponse.success(response, "파일이 업로드되었습니다"));
    }

    /**
     * 파일 다운로드/표시
     */
    @GetMapping("/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long fileId) throws IOException {
        FileInfoResponse fileInfo = fileService.getFileInfo(fileId);
        Resource resource = fileService.downloadFile(fileId);

        // 파일명 인코딩 (한글 지원)
        String encodedFilename = URLEncoder.encode(fileInfo.getOriginalName(), StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        // Content-Type 결정
        MediaType mediaType = MediaType.parseMediaType(
                fileInfo.getMimeType() != null ? fileInfo.getMimeType() : "application/octet-stream"
        );

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + encodedFilename + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=31536000")
                .body(resource);
    }

    /**
     * 파일 정보 조회
     */
    @GetMapping("/{fileId}/info")
    public ResponseEntity<ApiResponse<FileInfoResponse>> getFileInfo(@PathVariable Long fileId) {
        FileInfoResponse response = fileService.getFileInfo(fileId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 파일 삭제
     */
    @DeleteMapping("/{fileId}")
    public ResponseEntity<ApiResponse<Void>> deleteFile(@PathVariable Long fileId) {
        Long userId = SecurityUtils.getCurrentUserId();
        fileService.deleteFile(fileId, userId);
        return ResponseEntity.ok(ApiResponse.successWithMessage("파일이 삭제되었습니다"));
    }

    /**
     * 연관 파일 목록 조회
     */
    @GetMapping("/related/{relatedType}/{relatedId}")
    public ResponseEntity<ApiResponse<List<FileInfoResponse>>> getRelatedFiles(
            @PathVariable String relatedType,
            @PathVariable Long relatedId
    ) {
        List<FileInfoResponse> files = fileService.getRelatedFiles(relatedType, relatedId);
        return ResponseEntity.ok(ApiResponse.success(files));
    }
}
```

#### 3.2.10 DTO 클래스

**FileUploadResponse.java**
```java
package com.taskflow.dto.file;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FileUploadResponse {
    private Long fileId;
    private String originalName;
    private String storedName;
    private String extension;
    private String mimeType;
    private Long fileSize;
    private String storageType;
    private String url;
    private String markdown;
}
```

**FileInfoResponse.java**
```java
package com.taskflow.dto.file;

import com.taskflow.domain.FileEntity;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class FileInfoResponse {
    private Long fileId;
    private String originalName;
    private String storedName;
    private String extension;
    private String mimeType;
    private Long fileSize;
    private String fileSizeDisplay;
    private String storageType;
    private String relatedType;
    private Long relatedId;
    private String url;
    private LocalDateTime createdAt;
    private Long createdBy;
    private String createdByName;

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

    private static String formatFileSize(Long size) {
        if (size == null) return "0 B";
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024));
        return String.format("%.1f GB", size / (1024.0 * 1024 * 1024));
    }
}
```

---

### 3.3 application.yml 설정 추가

```yaml
# 기존 설정 아래에 추가

# 파일 스토리지 설정
storage:
  type: LOCAL
  max-file-size: 10MB
  allowed-extensions:
    - jpg
    - jpeg
    - png
    - gif
    - webp
    - svg
    - pdf
    - doc
    - docx
    - xls
    - xlsx
    - ppt
    - pptx
    - txt
    - zip

  local:
    base-path: /data/uploads
    url-prefix: /api/files

# Spring 파일 업로드 설정
spring:
  servlet:
    multipart:
      enabled: true
      max-file-size: 10MB
      max-request-size: 50MB
```

---

### 3.4 Docker Compose 수정

```yaml
services:
  backend:
    # ... 기존 설정
    volumes:
      - upload_data:/data/uploads
    environment:
      - STORAGE_TYPE=LOCAL
      - STORAGE_LOCAL_BASE_PATH=/data/uploads

volumes:
  mysql_data:
  upload_data:
```

---

## 4. 프론트엔드 개발 지시

### 4.1 파일 구조

```
frontend/src/
├── api/
│   └── file.ts                           [신규]
├── types/
│   └── file.ts                           [신규]
└── components/
    └── common/
        └── MarkdownEditor.vue            [수정]
```

### 4.2 구현 상세

#### 4.2.1 types/file.ts

```typescript
/**
 * 파일 관련 타입 정의
 */

export interface FileUploadResponse {
  fileId: number
  originalName: string
  storedName: string
  extension: string
  mimeType: string
  fileSize: number
  storageType: string
  url: string
  markdown: string
}

export interface FileInfo {
  fileId: number
  originalName: string
  storedName: string
  extension: string
  mimeType: string
  fileSize: number
  fileSizeDisplay: string
  storageType: string
  relatedType?: string
  relatedId?: number
  url: string
  createdAt: string
  createdBy: number
  createdByName: string
}

export interface FileUploadOptions {
  relatedType?: string
  relatedId?: number
  onProgress?: (progress: number) => void
}
```

#### 4.2.2 api/file.ts

```typescript
import { post, get, del } from './client'
import type { FileUploadResponse, FileInfo } from '@/types/file'
import type { ApiResponse } from '@/types/api'

/**
 * 파일 API
 */
export const fileApi = {
  /**
   * 파일 업로드
   */
  async upload(
    file: File,
    options?: {
      relatedType?: string
      relatedId?: number
      onProgress?: (progress: number) => void
    }
  ): Promise<FileUploadResponse> {
    const formData = new FormData()
    formData.append('file', file)

    if (options?.relatedType) {
      formData.append('relatedType', options.relatedType)
    }
    if (options?.relatedId) {
      formData.append('relatedId', String(options.relatedId))
    }

    const response = await post<ApiResponse<FileUploadResponse>>(
      '/files/upload',
      formData,
      {
        headers: {
          'Content-Type': 'multipart/form-data'
        },
        onUploadProgress: (progressEvent) => {
          if (options?.onProgress && progressEvent.total) {
            const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total)
            options.onProgress(progress)
          }
        }
      }
    )

    if (!response.success || !response.data) {
      throw new Error(response.message || '파일 업로드에 실패했습니다.')
    }

    return response.data
  },

  /**
   * 파일 URL 반환
   */
  getFileUrl(fileId: number): string {
    return `/api/files/${fileId}`
  },

  /**
   * 파일 정보 조회
   */
  async getFileInfo(fileId: number): Promise<FileInfo> {
    const response = await get<ApiResponse<FileInfo>>(`/files/${fileId}/info`)
    if (!response.success || !response.data) {
      throw new Error(response.message || '파일 정보를 가져올 수 없습니다.')
    }
    return response.data
  },

  /**
   * 파일 삭제
   */
  async deleteFile(fileId: number): Promise<void> {
    const response = await del<ApiResponse<void>>(`/files/${fileId}`)
    if (!response.success) {
      throw new Error(response.message || '파일 삭제에 실패했습니다.')
    }
  },

  /**
   * 연관 파일 목록 조회
   */
  async getRelatedFiles(relatedType: string, relatedId: number): Promise<FileInfo[]> {
    const response = await get<ApiResponse<FileInfo[]>>(`/files/related/${relatedType}/${relatedId}`)
    if (!response.success || !response.data) {
      throw new Error(response.message || '파일 목록을 가져올 수 없습니다.')
    }
    return response.data
  }
}
```

#### 4.2.3 MarkdownEditor.vue 수정

```vue
<script setup lang="ts">
/**
 * 마크다운 에디터 컴포넌트
 * - 에디터/미리보기 탭 전환
 * - marked 라이브러리로 마크다운 렌더링
 * - 툴바로 간편 서식 적용
 * - 이미지 업로드 (드래그앤드롭, 클립보드, 버튼)
 */
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { marked } from 'marked'
import { fileApi } from '@/api/file'
import { useToast } from '@/composables/useToast'

interface Props {
  modelValue?: string
  placeholder?: string
  disabled?: boolean
  minHeight?: string
  autoSaveDelay?: number
  relatedType?: string
  relatedId?: number
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  placeholder: '마크다운 형식으로 내용을 입력하세요...',
  disabled: false,
  minHeight: '300px',
  autoSaveDelay: 1000
})

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
  (e: 'save', value: string): void
}>()

const toast = useToast()

// 상태
const activeTab = ref<'edit' | 'preview'>('edit')
const localValue = ref(props.modelValue || '')
const textareaRef = ref<HTMLTextAreaElement | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)
const saveTimer = ref<ReturnType<typeof setTimeout> | null>(null)

// 업로드 상태
const isUploading = ref(false)
const uploadProgress = ref(0)
const uploadFileName = ref('')

// 허용 이미지 타입
const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'image/svg+xml']
const MAX_FILE_SIZE = 10 * 1024 * 1024 // 10MB

// 마크다운 렌더링
const renderedContent = computed(() => {
  if (!localValue.value) return '<p class="text-gray-400">미리보기할 내용이 없습니다.</p>'
  try {
    return marked(localValue.value, { breaks: true, gfm: true })
  } catch {
    return '<p class="text-red-500">마크다운 렌더링 오류</p>'
  }
})

// props 변경 감지
watch(() => props.modelValue, (newVal) => {
  if (newVal !== localValue.value) {
    localValue.value = newVal || ''
  }
})

// 입력 처리 (디바운스 자동 저장)
function handleInput(event: Event) {
  const target = event.target as HTMLTextAreaElement
  localValue.value = target.value
  emit('update:modelValue', target.value)

  if (saveTimer.value) {
    clearTimeout(saveTimer.value)
  }
  saveTimer.value = setTimeout(() => {
    emit('save', localValue.value)
  }, props.autoSaveDelay)
}

// blur 시 즉시 저장
function handleBlur() {
  if (saveTimer.value) {
    clearTimeout(saveTimer.value)
    saveTimer.value = null
  }
  emit('save', localValue.value)
}

// =============================================
// 이미지 업로드 기능
// =============================================

/**
 * 이미지 버튼 클릭 - 파일 선택 다이얼로그
 */
function openImageDialog() {
  fileInputRef.value?.click()
}

/**
 * 파일 선택 핸들러
 */
function handleFileSelect(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (file) {
    uploadImage(file)
  }
  // 같은 파일 재선택을 위해 초기화
  input.value = ''
}

/**
 * 드래그 앤 드롭 핸들러
 */
function handleDrop(event: DragEvent) {
  event.preventDefault()
  if (props.disabled || isUploading.value) return

  const file = event.dataTransfer?.files?.[0]
  if (file && ALLOWED_IMAGE_TYPES.includes(file.type)) {
    uploadImage(file)
  }
}

function handleDragOver(event: DragEvent) {
  event.preventDefault()
}

/**
 * 클립보드 붙여넣기 핸들러
 */
function handlePaste(event: ClipboardEvent) {
  if (props.disabled || isUploading.value) return

  const items = event.clipboardData?.items
  if (!items) return

  for (const item of items) {
    if (item.type.startsWith('image/')) {
      event.preventDefault()
      const file = item.getAsFile()
      if (file) {
        uploadImage(file)
      }
      break
    }
  }
}

/**
 * 이미지 업로드 실행
 */
async function uploadImage(file: File) {
  // 유효성 검사
  if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
    toast.error('지원하지 않는 이미지 형식입니다.')
    return
  }

  if (file.size > MAX_FILE_SIZE) {
    toast.error('파일 크기가 10MB를 초과합니다.')
    return
  }

  isUploading.value = true
  uploadProgress.value = 0
  uploadFileName.value = file.name

  try {
    const result = await fileApi.upload(file, {
      relatedType: props.relatedType,
      relatedId: props.relatedId,
      onProgress: (progress) => {
        uploadProgress.value = progress
      }
    })

    // 마크다운 이미지 문법 삽입
    insertImageMarkdown(result.url, result.originalName)
    toast.success('이미지가 업로드되었습니다.')
  } catch (error: any) {
    toast.error(error.message || '이미지 업로드에 실패했습니다.')
  } finally {
    isUploading.value = false
    uploadProgress.value = 0
    uploadFileName.value = ''
  }
}

/**
 * 마크다운 이미지 문법 삽입
 */
function insertImageMarkdown(url: string, altText: string) {
  if (!textareaRef.value) return

  const textarea = textareaRef.value
  const start = textarea.selectionStart
  const end = textarea.selectionEnd

  const imageMarkdown = `![${altText}](${url})`
  const newValue = localValue.value.substring(0, start)
    + imageMarkdown
    + localValue.value.substring(end)

  localValue.value = newValue
  emit('update:modelValue', newValue)

  // 커서 위치 조정
  setTimeout(() => {
    textarea.focus()
    const newPosition = start + imageMarkdown.length
    textarea.setSelectionRange(newPosition, newPosition)
  }, 0)
}

// =============================================
// 기존 툴바 기능
// =============================================

function wrapText(before: string, after: string) {
  if (!textareaRef.value || props.disabled) return

  const textarea = textareaRef.value
  const start = textarea.selectionStart
  const end = textarea.selectionEnd
  const selectedText = localValue.value.substring(start, end)

  const newText = localValue.value.substring(0, start)
    + before + selectedText + after
    + localValue.value.substring(end)

  localValue.value = newText
  emit('update:modelValue', newText)

  setTimeout(() => {
    textarea.focus()
    if (selectedText) {
      textarea.setSelectionRange(start + before.length, end + before.length)
    } else {
      textarea.setSelectionRange(start + before.length, start + before.length)
    }
  }, 0)
}

function insertAtLineStart(prefix: string) {
  if (!textareaRef.value || props.disabled) return

  const textarea = textareaRef.value
  const start = textarea.selectionStart

  let lineStart = start
  while (lineStart > 0 && localValue.value[lineStart - 1] !== '\n') {
    lineStart--
  }

  const newText = localValue.value.substring(0, lineStart)
    + prefix
    + localValue.value.substring(lineStart)

  localValue.value = newText
  emit('update:modelValue', newText)

  setTimeout(() => {
    textarea.focus()
    textarea.setSelectionRange(start + prefix.length, start + prefix.length)
  }, 0)
}

// 툴바 버튼들
const toolbarButtons = [
  { icon: 'B', action: () => wrapText('**', '**'), title: '굵게 (Ctrl+B)' },
  { icon: 'I', action: () => wrapText('*', '*'), title: '기울임 (Ctrl+I)' },
  { icon: '~', action: () => wrapText('~~', '~~'), title: '취소선' },
  { icon: 'H1', action: () => insertAtLineStart('# '), title: '제목 1' },
  { icon: 'H2', action: () => insertAtLineStart('## '), title: '제목 2' },
  { icon: 'H3', action: () => insertAtLineStart('### '), title: '제목 3' },
  { icon: '-', action: () => insertAtLineStart('- '), title: '목록' },
  { icon: '1.', action: () => insertAtLineStart('1. '), title: '번호 목록' },
  { icon: '[ ]', action: () => insertAtLineStart('- [ ] '), title: '체크박스' },
  { icon: '`', action: () => wrapText('`', '`'), title: '인라인 코드' },
  { icon: '```', action: () => wrapText('```\n', '\n```'), title: '코드 블록' },
  { icon: '>', action: () => insertAtLineStart('> '), title: '인용' },
  { icon: '---', action: () => insertAtLineStart('\n---\n'), title: '구분선' },
]

// 키보드 단축키
function handleKeydown(event: KeyboardEvent) {
  if (props.disabled) return

  if (event.ctrlKey || event.metaKey) {
    switch (event.key.toLowerCase()) {
      case 'b':
        event.preventDefault()
        wrapText('**', '**')
        break
      case 'i':
        event.preventDefault()
        wrapText('*', '*')
        break
    }
  }
}

// 컴포넌트 언마운트 시 타이머 정리
onUnmounted(() => {
  if (saveTimer.value) {
    clearTimeout(saveTimer.value)
  }
})
</script>

<template>
  <div class="markdown-editor border border-gray-300 rounded-lg overflow-hidden bg-white h-full flex flex-col">
    <!-- 헤더: 탭 + 툴바 -->
    <div class="flex items-center justify-between border-b border-gray-200 bg-gray-50 px-2 py-1">
      <!-- 탭 -->
      <div class="flex gap-1">
        <button
          type="button"
          class="px-3 py-1.5 text-[12px] font-medium rounded transition-colors"
          :class="activeTab === 'edit'
            ? 'bg-white text-gray-900 shadow-sm'
            : 'text-gray-500 hover:text-gray-700'"
          @click="activeTab = 'edit'"
        >
          편집
        </button>
        <button
          type="button"
          class="px-3 py-1.5 text-[12px] font-medium rounded transition-colors"
          :class="activeTab === 'preview'
            ? 'bg-white text-gray-900 shadow-sm'
            : 'text-gray-500 hover:text-gray-700'"
          @click="activeTab = 'preview'"
        >
          미리보기
        </button>
      </div>

      <!-- 툴바 (편집 모드에서만) -->
      <div v-if="activeTab === 'edit'" class="flex items-center gap-0.5">
        <button
          v-for="btn in toolbarButtons"
          :key="btn.icon"
          type="button"
          class="px-1.5 py-1 text-[11px] font-mono text-gray-600 hover:text-gray-900 hover:bg-gray-200 rounded transition-colors"
          :class="{ 'opacity-50 cursor-not-allowed': disabled }"
          :title="btn.title"
          :disabled="disabled"
          @click="btn.action"
        >
          {{ btn.icon }}
        </button>

        <!-- 구분선 -->
        <div class="w-px h-4 bg-gray-300 mx-1"></div>

        <!-- 이미지 업로드 버튼 -->
        <button
          type="button"
          class="px-1.5 py-1 text-gray-600 hover:text-gray-900 hover:bg-gray-200 rounded transition-colors"
          :class="{ 'opacity-50 cursor-not-allowed': disabled || isUploading }"
          title="이미지 업로드"
          :disabled="disabled || isUploading"
          @click="openImageDialog"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
          </svg>
        </button>

        <!-- 숨겨진 파일 입력 -->
        <input
          ref="fileInputRef"
          type="file"
          accept="image/*"
          class="hidden"
          @change="handleFileSelect"
        />
      </div>
    </div>

    <!-- 컨텐츠 영역 -->
    <div
      class="flex-1 min-h-0 overflow-hidden relative"
      :style="{ minHeight }"
      @drop="handleDrop"
      @dragover="handleDragOver"
    >
      <!-- 편집 모드 -->
      <textarea
        v-show="activeTab === 'edit'"
        ref="textareaRef"
        :value="localValue"
        :placeholder="placeholder"
        :disabled="disabled"
        class="w-full h-full p-3 text-[13px] font-mono resize-none focus:outline-none disabled:bg-gray-100 disabled:cursor-not-allowed"
        @input="handleInput"
        @blur="handleBlur"
        @keydown="handleKeydown"
        @paste="handlePaste"
      />

      <!-- 미리보기 모드 -->
      <div
        v-show="activeTab === 'preview'"
        class="prose prose-sm max-w-none p-3 h-full overflow-auto"
        v-html="renderedContent"
      />

      <!-- 업로드 진행률 표시 -->
      <div
        v-if="isUploading"
        class="absolute bottom-0 left-0 right-0 bg-white border-t border-gray-200 px-3 py-2"
      >
        <div class="flex items-center gap-2">
          <svg class="animate-spin h-4 w-4 text-primary-600" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
          </svg>
          <span class="text-[12px] text-gray-600 truncate flex-1">
            {{ uploadFileName }} 업로드 중...
          </span>
          <span class="text-[12px] font-medium text-primary-600">{{ uploadProgress }}%</span>
        </div>
        <div class="mt-1 h-1 bg-gray-200 rounded-full overflow-hidden">
          <div
            class="h-full bg-primary-600 transition-all duration-300"
            :style="{ width: `${uploadProgress}%` }"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 기존 스타일 유지 */
.prose {
  @apply text-gray-900;
}

/* 이미지 스타일 추가 */
.prose :deep(img) {
  @apply max-w-full h-auto rounded-lg shadow-sm my-2;
}

/* 나머지 기존 스타일 ... */
</style>
```

---

## 5. 데이터베이스 스크립트

### 5.1 01_schema.sql에 추가

```sql
-- ============================================
-- 파일 메타데이터 테이블
-- ============================================
CREATE TABLE TB_FILE (
    FILE_ID         BIGINT          PRIMARY KEY AUTO_INCREMENT COMMENT '파일 ID',

    -- 원본 파일 정보
    ORIGINAL_NAME   VARCHAR(255)    NOT NULL COMMENT '원본 파일명',
    STORED_NAME     VARCHAR(255)    NOT NULL COMMENT '저장된 파일명 (UUID)',
    EXTENSION       VARCHAR(20)     NULL COMMENT '파일 확장자',
    MIME_TYPE       VARCHAR(100)    NULL COMMENT 'MIME 타입',
    FILE_SIZE       BIGINT          NOT NULL DEFAULT 0 COMMENT '파일 크기 (bytes)',

    -- 스토리지 정보
    STORAGE_TYPE    VARCHAR(20)     NOT NULL DEFAULT 'LOCAL' COMMENT 'LOCAL, NAS, S3',
    STORAGE_PATH    VARCHAR(500)    NOT NULL COMMENT '스토리지 내 상대 경로',
    STORAGE_BUCKET  VARCHAR(100)    NULL COMMENT 'S3 버킷명 또는 NAS 공유명',

    -- 연관 정보
    RELATED_TYPE    VARCHAR(50)     NULL COMMENT 'ITEM, COMMENT, USER 등',
    RELATED_ID      BIGINT          NULL COMMENT '연관 엔티티 ID',

    -- 상태
    USE_YN          CHAR(1)         NOT NULL DEFAULT 'Y' COMMENT '사용 여부',

    -- 공통
    CREATED_AT      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CREATED_BY      BIGINT          NOT NULL,
    UPDATED_AT      DATETIME        NULL ON UPDATE CURRENT_TIMESTAMP,
    UPDATED_BY      BIGINT          NULL,

    INDEX IDX_FILE_RELATED (RELATED_TYPE, RELATED_ID),
    INDEX IDX_FILE_STORAGE (STORAGE_TYPE, STORAGE_PATH(100)),
    INDEX IDX_FILE_CREATED (CREATED_BY, CREATED_AT)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='파일 메타데이터';
```

---

## 6. 테스트 체크리스트

### 6.1 백엔드 테스트

- [ ] 파일 업로드 API 동작 확인
- [ ] 이미지 파일 업로드 성공
- [ ] 지원하지 않는 확장자 거부
- [ ] 파일 크기 초과 거부
- [ ] 파일 다운로드 API 동작 확인
- [ ] 파일 정보 조회 API 동작 확인
- [ ] 파일 삭제 API 동작 확인
- [ ] 권한 없는 사용자 삭제 거부
- [ ] 연관 파일 목록 조회 동작 확인

### 6.2 프론트엔드 테스트

- [ ] 이미지 버튼 클릭 → 파일 선택 → 업로드 성공
- [ ] 드래그 앤 드롭 업로드 성공
- [ ] 클립보드 붙여넣기 업로드 성공
- [ ] 업로드 진행률 표시
- [ ] 마크다운 이미지 문법 자동 삽입
- [ ] 미리보기에서 이미지 렌더링
- [ ] 에러 시 토스트 메시지 표시

### 6.3 통합 테스트

- [ ] 업무 설명에 이미지 추가 → 저장 → 재조회 시 이미지 표시
- [ ] 이미지 삭제 후 재조회 시 이미지 없음
- [ ] Docker 재시작 후 업로드된 이미지 유지

---

## 7. 개발 일정

| 단계 | 작업 내용 | 예상 소요 |
|------|----------|----------|
| Phase 1-1 | DB 테이블 생성 | - |
| Phase 1-2 | 스토리지 인터페이스 및 LocalFileStorage 구현 | - |
| Phase 1-3 | FileService, FileMapper 구현 | - |
| Phase 1-4 | FileController 구현 | - |
| Phase 2-1 | 프론트엔드 file API 구현 | - |
| Phase 2-2 | MarkdownEditor 이미지 업로드 기능 추가 | - |
| Phase 3 | 통합 테스트 및 버그 수정 | - |

---

## 8. 참고 사항

### 8.1 주의 사항
- 파일 경로에 `..` 사용 금지 (경로 조작 공격 방지)
- 파일명에 특수문자 제거 (UUID로 저장)
- 대용량 파일 스트리밍 처리 (메모리 효율)

### 8.2 확장 계획
- Phase 4: NAS 스토리지 구현체 추가
- Phase 5: S3 스토리지 구현체 추가
- Phase 6: 이미지 리사이징/썸네일 생성
- Phase 7: CDN 연동

---

## 9. 승인

| 역할 | 이름 | 날짜 | 서명 |
|------|------|------|------|
| 개발자 | | | |
| 검토자 | | | |
| 승인자 | | | |
