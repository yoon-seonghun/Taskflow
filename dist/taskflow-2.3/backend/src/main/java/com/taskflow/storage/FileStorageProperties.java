package com.taskflow.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 파일 스토리지 설정
 */
@Component
@ConfigurationProperties(prefix = "file.storage")
@Getter
@Setter
public class FileStorageProperties {

    /**
     * 활성 스토리지 타입 (LOCAL, NAS, S3)
     */
    private StorageType type = StorageType.LOCAL;

    /**
     * 최대 파일 크기 (예: 10MB, 1GB)
     */
    private String maxFileSize = "10MB";

    /**
     * 허용 확장자 목록
     */
    private List<String> allowedExtensions;

    /**
     * 로컬 스토리지 설정
     */
    private LocalStorage local = new LocalStorage();

    /**
     * SFTP 스토리지 설정
     */
    private SftpStorage sftp = new SftpStorage();

    /**
     * NAS 스토리지 설정 (추후 구현)
     */
    private NasStorage nas = new NasStorage();

    /**
     * S3 스토리지 설정 (추후 구현)
     */
    private S3Storage s3 = new S3Storage();

    @Getter
    @Setter
    public static class LocalStorage {
        /**
         * 파일 저장 기본 경로
         */
        private String basePath = "/data/uploads";

        /**
         * 파일 접근 URL prefix
         */
        private String urlPrefix = "/api/files";
    }

    @Getter
    @Setter
    public static class NasStorage {
        /**
         * NAS 마운트 경로
         */
        private String basePath;

        /**
         * NAS 공유명
         */
        private String shareName;

        /**
         * 파일 접근 URL prefix
         */
        private String urlPrefix = "/api/files";
    }

    @Getter
    @Setter
    public static class SftpStorage {
        /**
         * SFTP 서버 호스트
         */
        private String host;

        /**
         * SFTP 서버 포트 (기본값: 22)
         */
        private int port = 22;

        /**
         * SFTP 사용자명
         */
        private String username;

        /**
         * SFTP 비밀번호 (비밀번호 인증 시)
         */
        private String password;

        /**
         * SSH 개인키 파일 경로 (키 인증 시)
         */
        private String privateKeyPath;

        /**
         * SSH 개인키 패스프레이즈 (선택)
         */
        private String privateKeyPassphrase;

        /**
         * 원격 서버 기본 경로 (업로드 디렉토리)
         */
        private String basePath = "/data/uploads";

        /**
         * 연결 타임아웃 (밀리초)
         */
        private int connectionTimeout = 30000;

        /**
         * 채널 타임아웃 (밀리초)
         */
        private int channelTimeout = 60000;

        /**
         * Strict Host Key Checking 여부
         */
        private boolean strictHostKeyChecking = false;

        /**
         * Known Hosts 파일 경로
         */
        private String knownHostsPath;

        /**
         * 파일 접근 URL prefix (웹 서버를 통한 접근 시)
         */
        private String urlPrefix = "/api/files";

        /**
         * 연결 풀 크기
         */
        private int poolSize = 5;

        /**
         * 연결 유지 시간 (밀리초)
         */
        private long keepAliveInterval = 30000;
    }

    @Getter
    @Setter
    public static class S3Storage {
        /**
         * S3 엔드포인트 URL
         */
        private String endpoint;

        /**
         * S3 버킷명
         */
        private String bucket;

        /**
         * AWS 리전
         */
        private String region;

        /**
         * Access Key
         */
        private String accessKey;

        /**
         * Secret Key
         */
        private String secretKey;

        /**
         * 파일 접근 URL prefix (CDN 또는 직접 접근)
         */
        private String urlPrefix;
    }

    /**
     * 파일 크기 문자열을 바이트로 변환
     */
    public long getMaxFileSizeBytes() {
        String size = maxFileSize.toUpperCase().trim();
        try {
            if (size.endsWith("KB")) {
                return Long.parseLong(size.replace("KB", "").trim()) * 1024;
            } else if (size.endsWith("MB")) {
                return Long.parseLong(size.replace("MB", "").trim()) * 1024 * 1024;
            } else if (size.endsWith("GB")) {
                return Long.parseLong(size.replace("GB", "").trim()) * 1024 * 1024 * 1024;
            } else {
                return Long.parseLong(size);
            }
        } catch (NumberFormatException e) {
            return 10 * 1024 * 1024; // 기본값 10MB
        }
    }
}
