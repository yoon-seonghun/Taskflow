package com.taskflow.controller;

import com.taskflow.common.ApiResponse;
import com.taskflow.domain.FileEntity;
import com.taskflow.dto.file.FileInfoResponse;
import com.taskflow.dto.file.FileUploadResponse;
import com.taskflow.security.UserPrincipal;
import com.taskflow.service.FileService;
import com.taskflow.storage.FileStorageService;
import com.taskflow.storage.FileStorageService.StorageHealthStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 파일 관리 REST API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final FileStorageService fileStorageService;

    /**
     * 파일 업로드
     *
     * @param file        업로드할 파일
     * @param relatedType 연관 엔티티 타입 (ITEM, COMMENT 등) - 선택
     * @param relatedId   연관 엔티티 ID - 선택
     * @return 업로드 결과
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "relatedType", required = false) String relatedType,
            @RequestParam(value = "relatedId", required = false) Long relatedId,
            @AuthenticationPrincipal UserPrincipal user
    ) throws IOException {
        // 인증 확인
        if (user == null) {
            log.warn("File upload failed: user not authenticated");
            return ResponseEntity.status(401).body(ApiResponse.error("인증이 필요합니다."));
        }

        log.info("File upload request: name={}, size={}, type={}, relatedType={}, relatedId={}",
                file.getOriginalFilename(), file.getSize(), file.getContentType(), relatedType, relatedId);

        FileUploadResponse response = fileService.uploadFile(file, relatedType, relatedId, user.getUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 파일 다운로드/표시
     *
     * @param fileId    파일 ID
     * @param download  다운로드 여부 (true: 다운로드, false/생략: 인라인 표시)
     * @return 파일 리소스
     */
    @GetMapping("/{fileId}")
    public ResponseEntity<Resource> getFile(
            @PathVariable Long fileId,
            @RequestParam(value = "download", defaultValue = "false") boolean download
    ) throws IOException {
        FileEntity fileEntity = fileService.getFileEntity(fileId);
        Resource resource = fileService.downloadFile(fileId);

        // Content-Disposition 설정
        String contentDisposition;
        if (download) {
            // 다운로드 모드: attachment
            String encodedFilename = URLEncoder.encode(fileEntity.getOriginalName(), StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
            contentDisposition = "attachment; filename*=UTF-8''" + encodedFilename;
        } else {
            // 인라인 모드: inline (브라우저에서 직접 표시)
            contentDisposition = "inline";
        }

        // MIME 타입 결정
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
        if (fileEntity.getMimeType() != null) {
            try {
                mediaType = MediaType.parseMediaType(fileEntity.getMimeType());
            } catch (Exception e) {
                log.warn("Failed to parse mime type: {}", fileEntity.getMimeType());
            }
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .body(resource);
    }

    /**
     * 파일 정보 조회
     *
     * @param fileId 파일 ID
     * @return 파일 메타데이터
     */
    @GetMapping("/{fileId}/info")
    public ResponseEntity<ApiResponse<FileInfoResponse>> getFileInfo(@PathVariable Long fileId) {
        FileInfoResponse response = fileService.getFileInfo(fileId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 파일 삭제
     *
     * @param fileId 파일 ID
     * @return 삭제 결과
     */
    @DeleteMapping("/{fileId}")
    public ResponseEntity<ApiResponse<Void>> deleteFile(
            @PathVariable Long fileId,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        // 인증 확인
        if (user == null) {
            log.warn("File delete failed: user not authenticated");
            return ResponseEntity.status(401).body(ApiResponse.error("인증이 필요합니다."));
        }

        fileService.deleteFile(fileId, user.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 연관 엔티티의 파일 목록 조회
     *
     * @param relatedType 연관 엔티티 타입
     * @param relatedId   연관 엔티티 ID
     * @return 파일 목록
     */
    @GetMapping("/related/{relatedType}/{relatedId}")
    public ResponseEntity<ApiResponse<List<FileInfoResponse>>> getRelatedFiles(
            @PathVariable String relatedType,
            @PathVariable Long relatedId
    ) {
        List<FileInfoResponse> files = fileService.getRelatedFiles(relatedType, relatedId);
        return ResponseEntity.ok(ApiResponse.success(files));
    }

    /**
     * 파일 연관 정보 업데이트
     * (업로드 후 연관 엔티티 저장 시 사용)
     *
     * @param fileId      파일 ID
     * @param relatedType 연관 엔티티 타입
     * @param relatedId   연관 엔티티 ID
     * @return 업데이트 결과
     */
    @PutMapping("/{fileId}/related")
    public ResponseEntity<ApiResponse<Void>> updateRelated(
            @PathVariable Long fileId,
            @RequestParam("relatedType") String relatedType,
            @RequestParam("relatedId") Long relatedId,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        // 인증 확인
        if (user == null) {
            log.warn("File update related failed: user not authenticated");
            return ResponseEntity.status(401).body(ApiResponse.error("인증이 필요합니다."));
        }

        fileService.updateRelated(fileId, relatedType, relatedId, user.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 이미지 파일 여부 확인
     *
     * @param fileId 파일 ID
     * @return 이미지 여부
     */
    @GetMapping("/{fileId}/is-image")
    public ResponseEntity<ApiResponse<Boolean>> isImage(@PathVariable Long fileId) {
        boolean isImage = fileService.isImageFile(fileId);
        return ResponseEntity.ok(ApiResponse.success(isImage));
    }

    /**
     * 스토리지 상태 확인
     * 현재 활성화된 스토리지(LOCAL/SFTP)의 연결 상태를 확인합니다.
     *
     * @return 스토리지 상태 정보
     */
    @GetMapping("/storage/health")
    public ResponseEntity<ApiResponse<StorageHealthStatus>> checkStorageHealth() {
        StorageHealthStatus status = fileStorageService.checkHealth();

        if (status.healthy()) {
            return ResponseEntity.ok(ApiResponse.success(status));
        } else {
            return ResponseEntity.status(503)
                    .body(ApiResponse.error(status.message()));
        }
    }

    /**
     * 현재 스토리지 타입 조회
     *
     * @return 스토리지 타입 (LOCAL, SFTP, NAS, S3)
     */
    @GetMapping("/storage/type")
    public ResponseEntity<ApiResponse<String>> getStorageType() {
        String storageType = fileStorageService.getStorageType().getCode();
        return ResponseEntity.ok(ApiResponse.success(storageType));
    }
}
