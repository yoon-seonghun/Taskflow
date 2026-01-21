package com.taskflow.storage.impl;

import com.jcraft.jsch.*;
import com.taskflow.storage.FileStorageProperties;
import com.taskflow.storage.FileStorageService;
import com.taskflow.storage.StorageType;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * SFTP 파일 스토리지 구현체
 * JSch 라이브러리를 사용하여 SFTP 프로토콜로 외부 서버에 파일 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "file.storage.type", havingValue = "SFTP")
public class SftpFileStorage implements FileStorageService {

    private final FileStorageProperties properties;
    private BlockingQueue<Session> sessionPool;
    private JSch jsch;

    @PostConstruct
    public void init() {
        FileStorageProperties.SftpStorage sftp = properties.getSftp();

        // SFTP 설정 유효성 검사 - host가 필수
        if (sftp.getHost() == null || sftp.getHost().isBlank()) {
            throw new IllegalStateException(
                "SFTP storage is enabled but host is not configured. " +
                "Please set 'file.storage.sftp.host' property or change storage type to LOCAL."
            );
        }

        log.info("Initializing SFTP file storage: {}@{}:{}",
                sftp.getUsername(), sftp.getHost(), sftp.getPort());
        if (sftp.getHomePath() != null && !sftp.getHomePath().isBlank()) {
            log.info("SFTP home path: {}", sftp.getHomePath());
        }
        log.info("SFTP base path: {}", sftp.getBasePath());

        jsch = new JSch();

        try {
            // SSH 키 인증 설정
            if (sftp.getPrivateKeyPath() != null && !sftp.getPrivateKeyPath().isEmpty()) {
                if (sftp.getPrivateKeyPassphrase() != null && !sftp.getPrivateKeyPassphrase().isEmpty()) {
                    jsch.addIdentity(sftp.getPrivateKeyPath(), sftp.getPrivateKeyPassphrase());
                } else {
                    jsch.addIdentity(sftp.getPrivateKeyPath());
                }
                log.info("SSH private key loaded from: {}", sftp.getPrivateKeyPath());
            }

            // Known Hosts 설정
            if (sftp.getKnownHostsPath() != null && !sftp.getKnownHostsPath().isEmpty()) {
                jsch.setKnownHosts(sftp.getKnownHostsPath());
                log.info("Known hosts loaded from: {}", sftp.getKnownHostsPath());
            }

            // 연결 풀 초기화
            sessionPool = new ArrayBlockingQueue<>(sftp.getPoolSize());

            // 초기 연결 테스트
            Session testSession = createSession();
            testSession.connect(sftp.getConnectionTimeout());

            // 기본 디렉토리 존재 확인 및 생성
            ensureBaseDirectoryExists(testSession);

            testSession.disconnect();
            log.info("SFTP file storage initialized successfully. Full base path: {}", getFullBasePath());

        } catch (JSchException e) {
            throw new RuntimeException("Failed to initialize SFTP storage: " + e.getMessage(), e);
        }
    }

    @PreDestroy
    public void cleanup() {
        log.info("Cleaning up SFTP session pool...");
        while (!sessionPool.isEmpty()) {
            try {
                Session session = sessionPool.poll(1, TimeUnit.SECONDS);
                if (session != null && session.isConnected()) {
                    session.disconnect();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        log.info("SFTP session pool cleaned up");
    }

    @Override
    public String upload(MultipartFile file, String directory) throws IOException {
        Session session = null;
        ChannelSftp channel = null;

        try {
            session = getSession();
            channel = openSftpChannel(session);

            // 원격 디렉토리 경로 생성 (homePath + basePath + directory)
            String remoteDirPath = getFullBasePath() + "/" + directory;
            ensureRemoteDirectoryExists(channel, remoteDirPath);

            // 고유 파일명 생성
            String originalFilename = file.getOriginalFilename();
            String extension = getExtension(originalFilename);
            String storedFilename = UUID.randomUUID().toString();
            if (!extension.isEmpty()) {
                storedFilename += "." + extension;
            }

            // 원격 파일 경로
            String remoteFilePath = remoteDirPath + "/" + storedFilename;

            // 파일 업로드
            try (InputStream inputStream = file.getInputStream()) {
                channel.put(inputStream, remoteFilePath, ChannelSftp.OVERWRITE);
            }

            // 상대 경로 반환
            String storagePath = directory + "/" + storedFilename;
            log.info("File uploaded via SFTP: {} -> {}", originalFilename, storagePath);

            return storagePath;

        } catch (JSchException | SftpException e) {
            throw new IOException("Failed to upload file via SFTP: " + e.getMessage(), e);
        } finally {
            closeSftpChannel(channel);
            returnSession(session);
        }
    }

    @Override
    public Resource download(String storagePath) throws IOException {
        Session session = null;
        ChannelSftp channel = null;

        try {
            session = getSession();
            channel = openSftpChannel(session);

            String remoteFilePath = getFullBasePath() + "/" + storagePath;

            // 파일 존재 확인
            try {
                channel.lstat(remoteFilePath);
            } catch (SftpException e) {
                if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                    throw new IOException("File not found: " + storagePath);
                }
                throw e;
            }

            // 파일 다운로드
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            channel.get(remoteFilePath, outputStream);

            byte[] fileContent = outputStream.toByteArray();
            log.debug("File downloaded via SFTP: {} ({} bytes)", storagePath, fileContent.length);

            return new ByteArrayResource(fileContent) {
                @Override
                public String getFilename() {
                    return storagePath.substring(storagePath.lastIndexOf("/") + 1);
                }
            };

        } catch (JSchException | SftpException e) {
            throw new IOException("Failed to download file via SFTP: " + e.getMessage(), e);
        } finally {
            closeSftpChannel(channel);
            returnSession(session);
        }
    }

    @Override
    public boolean delete(String storagePath) {
        Session session = null;
        ChannelSftp channel = null;

        try {
            session = getSession();
            channel = openSftpChannel(session);

            String remoteFilePath = getFullBasePath() + "/" + storagePath;

            // 파일 삭제
            channel.rm(remoteFilePath);
            log.info("File deleted via SFTP: {}", storagePath);

            // 빈 디렉토리 정리 시도
            cleanupEmptyDirectories(channel, remoteFilePath);

            return true;

        } catch (JSchException | SftpException e) {
            if (e instanceof SftpException && ((SftpException) e).id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                log.warn("File not found for deletion: {}", storagePath);
                return false;
            }
            log.error("Failed to delete file via SFTP: {}", storagePath, e);
            return false;
        } finally {
            closeSftpChannel(channel);
            returnSession(session);
        }
    }

    @Override
    public boolean exists(String storagePath) {
        Session session = null;
        ChannelSftp channel = null;

        try {
            session = getSession();
            channel = openSftpChannel(session);

            String remoteFilePath = getFullBasePath() + "/" + storagePath;

            channel.lstat(remoteFilePath);
            return true;

        } catch (JSchException e) {
            log.error("SFTP connection error while checking file existence: {}", storagePath, e);
            return false;
        } catch (SftpException e) {
            if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                return false;
            }
            log.error("SFTP error while checking file existence: {}", storagePath, e);
            return false;
        } finally {
            closeSftpChannel(channel);
            returnSession(session);
        }
    }

    @Override
    public String getAccessUrl(String storagePath) {
        // SFTP의 경우 FileController를 통해 접근
        return properties.getSftp().getUrlPrefix();
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.SFTP;
    }

    @Override
    public StorageHealthStatus checkHealth() {
        FileStorageProperties.SftpStorage sftp = properties.getSftp();
        long startTime = System.currentTimeMillis();
        Session session = null;
        ChannelSftp channel = null;
        String fullBasePath = getFullBasePath();

        try {
            session = getSession();
            channel = openSftpChannel(session);

            // 기본 디렉토리 접근 확인
            channel.lstat(fullBasePath);

            // 테스트 파일 쓰기/삭제
            String testFilePath = fullBasePath + "/.health-check-" + System.currentTimeMillis();
            channel.put(new java.io.ByteArrayInputStream("health-check".getBytes()), testFilePath);
            channel.rm(testFilePath);

            long responseTime = System.currentTimeMillis() - startTime;

            return new StorageHealthStatus(
                    true,
                    StorageType.SFTP,
                    String.format("SFTP storage is healthy. Host: %s:%d, Base path: %s",
                            sftp.getHost(), sftp.getPort(), fullBasePath),
                    responseTime
            );

        } catch (JSchException e) {
            return new StorageHealthStatus(
                    false,
                    StorageType.SFTP,
                    "SFTP connection failed: " + e.getMessage(),
                    System.currentTimeMillis() - startTime
            );
        } catch (SftpException e) {
            return new StorageHealthStatus(
                    false,
                    StorageType.SFTP,
                    "SFTP operation failed: " + e.getMessage(),
                    System.currentTimeMillis() - startTime
            );
        } finally {
            closeSftpChannel(channel);
            returnSession(session);
        }
    }

    /**
     * SFTP 세션 생성
     */
    private Session createSession() throws JSchException {
        FileStorageProperties.SftpStorage sftp = properties.getSftp();

        Session session = jsch.getSession(sftp.getUsername(), sftp.getHost(), sftp.getPort());

        // 비밀번호 인증
        if (sftp.getPassword() != null && !sftp.getPassword().isEmpty()) {
            session.setPassword(sftp.getPassword());
        }

        // SSH 설정
        Properties config = new Properties();
        if (!sftp.isStrictHostKeyChecking()) {
            config.put("StrictHostKeyChecking", "no");
        }
        config.put("PreferredAuthentications", "publickey,password,keyboard-interactive");
        session.setConfig(config);

        // Keep-alive 설정
        session.setServerAliveInterval((int) sftp.getKeepAliveInterval());

        return session;
    }

    /**
     * 세션 풀에서 세션 가져오기
     */
    private Session getSession() throws JSchException {
        FileStorageProperties.SftpStorage sftp = properties.getSftp();

        // 풀에서 가져오기 시도
        Session session = sessionPool.poll();

        if (session != null && session.isConnected()) {
            return session;
        }

        // 새 세션 생성
        session = createSession();
        session.connect(sftp.getConnectionTimeout());

        return session;
    }

    /**
     * 세션을 풀에 반환
     */
    private void returnSession(Session session) {
        if (session == null) {
            return;
        }

        if (session.isConnected()) {
            boolean offered = sessionPool.offer(session);
            if (!offered) {
                // 풀이 가득 찬 경우 세션 종료
                session.disconnect();
            }
        }
    }

    /**
     * SFTP 채널 열기
     */
    private ChannelSftp openSftpChannel(Session session) throws JSchException {
        FileStorageProperties.SftpStorage sftp = properties.getSftp();

        Channel channel = session.openChannel("sftp");
        channel.connect(sftp.getChannelTimeout());

        return (ChannelSftp) channel;
    }

    /**
     * SFTP 채널 닫기
     */
    private void closeSftpChannel(ChannelSftp channel) {
        if (channel != null && channel.isConnected()) {
            channel.disconnect();
        }
    }

    /**
     * 기본 디렉토리 존재 확인 및 생성
     */
    private void ensureBaseDirectoryExists(Session session) {
        ChannelSftp channel = null;

        try {
            channel = openSftpChannel(session);
            ensureRemoteDirectoryExists(channel, getFullBasePath());
        } catch (JSchException | SftpException e) {
            log.warn("Could not ensure base directory exists: {}", e.getMessage());
        } finally {
            closeSftpChannel(channel);
        }
    }

    /**
     * 원격 디렉토리 존재 확인 및 재귀적 생성
     */
    private void ensureRemoteDirectoryExists(ChannelSftp channel, String remotePath) throws SftpException {
        String[] parts = remotePath.split("/");
        StringBuilder currentPath = new StringBuilder();

        for (String part : parts) {
            if (part.isEmpty()) {
                currentPath.append("/");
                continue;
            }

            currentPath.append(part);

            try {
                channel.lstat(currentPath.toString());
            } catch (SftpException e) {
                if (e.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
                    channel.mkdir(currentPath.toString());
                    log.debug("Created remote directory: {}", currentPath);
                } else {
                    throw e;
                }
            }

            currentPath.append("/");
        }
    }

    /**
     * 빈 디렉토리 정리
     */
    private void cleanupEmptyDirectories(ChannelSftp channel, String filePath) {
        String fullBasePath = getFullBasePath();

        try {
            String dirPath = filePath.substring(0, filePath.lastIndexOf("/"));

            while (!dirPath.equals(fullBasePath) && dirPath.startsWith(fullBasePath)) {
                try {
                    @SuppressWarnings("unchecked")
                    java.util.Vector<ChannelSftp.LsEntry> entries = channel.ls(dirPath);

                    // . 과 .. 만 있으면 빈 디렉토리
                    if (entries.size() <= 2) {
                        channel.rmdir(dirPath);
                        log.debug("Removed empty directory: {}", dirPath);
                        dirPath = dirPath.substring(0, dirPath.lastIndexOf("/"));
                    } else {
                        break;
                    }
                } catch (SftpException e) {
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("Failed to cleanup empty directories: {}", e.getMessage());
        }
    }

    /**
     * 전체 기본 경로 반환 (homePath + basePath)
     * homePath가 설정되어 있으면 homePath/basePath 형태로 반환
     * homePath가 없으면 basePath만 반환
     */
    private String getFullBasePath() {
        FileStorageProperties.SftpStorage sftp = properties.getSftp();
        String homePath = sftp.getHomePath();
        String basePath = sftp.getBasePath();

        if (homePath != null && !homePath.isBlank()) {
            // homePath가 /로 끝나면 중복 제거
            if (homePath.endsWith("/")) {
                homePath = homePath.substring(0, homePath.length() - 1);
            }
            // basePath가 /로 시작하면 절대 경로로 간주하여 basePath만 사용
            if (basePath.startsWith("/")) {
                return basePath;
            }
            return homePath + "/" + basePath;
        }
        return basePath;
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
}
