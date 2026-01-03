package com.taskflow.service;

import com.taskflow.domain.AuditLog;
import com.taskflow.dto.audit.AuditLogPageResponse;
import com.taskflow.dto.audit.AuditLogResponse;
import com.taskflow.dto.audit.AuditLogSearchRequest;
import com.taskflow.mapper.AuditLogMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 감사 로그 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    /**
     * 로그 기록
     */
    @Transactional
    public void log(String targetType, Long targetId, String action,
                    String actorUsername, String description,
                    Object beforeData, Object afterData, String relatedUsername) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .targetType(targetType)
                    .targetId(targetId)
                    .action(action)
                    .actorUsername(actorUsername)
                    .description(description)
                    .beforeData(beforeData != null ? objectMapper.writeValueAsString(beforeData) : null)
                    .afterData(afterData != null ? objectMapper.writeValueAsString(afterData) : null)
                    .relatedUsername(relatedUsername)
                    .build();

            auditLogMapper.insert(auditLog);
            log.debug("Audit log recorded: {} {} on {} #{}", action, targetType, targetId, auditLog.getLogId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize audit log data", e);
            // 로그 기록 실패는 비즈니스 로직에 영향을 주지 않도록 예외를 삼킴
        }
    }

    /**
     * 보드 생성 로그
     */
    @Transactional
    public void logBoardCreated(Long boardId, String actorUsername, String boardName) {
        log(AuditLog.TARGET_BOARD, boardId, AuditLog.ACTION_CREATE,
                actorUsername, "보드 생성: " + boardName, null, null, null);
    }

    /**
     * 보드 수정 로그
     */
    @Transactional
    public void logBoardUpdated(Long boardId, String actorUsername, Object beforeData, Object afterData) {
        log(AuditLog.TARGET_BOARD, boardId, AuditLog.ACTION_UPDATE,
                actorUsername, "보드 수정", beforeData, afterData, null);
    }

    /**
     * 보드 삭제 로그
     */
    @Transactional
    public void logBoardDeleted(Long boardId, String actorUsername, String boardName) {
        log(AuditLog.TARGET_BOARD, boardId, AuditLog.ACTION_DELETE,
                actorUsername, "보드 삭제: " + boardName, null, null, null);
    }

    /**
     * 업무 이관 로그
     */
    @Transactional
    public void logItemTransferred(Long itemId, String actorUsername, String targetUsername,
                                    String fromBoardName, String toBoardName) {
        String description = String.format("업무 이관: %s → %s", fromBoardName, toBoardName);
        log(AuditLog.TARGET_ITEM, itemId, AuditLog.ACTION_TRANSFER,
                actorUsername, description, null, null, targetUsername);
    }

    /**
     * 보드 공유 추가 로그
     */
    @Transactional
    public void logBoardShared(Long boardId, String actorUsername, String targetUsername, String permission) {
        log(AuditLog.TARGET_BOARD_SHARE, boardId, AuditLog.ACTION_SHARE,
                actorUsername, "보드 공유 추가 (" + permission + ")", null, null, targetUsername);
    }

    /**
     * 보드 공유 해제 로그
     */
    @Transactional
    public void logBoardUnshared(Long boardId, String actorUsername, String targetUsername) {
        log(AuditLog.TARGET_BOARD_SHARE, boardId, AuditLog.ACTION_UNSHARE,
                actorUsername, "보드 공유 해제", null, null, targetUsername);
    }

    /**
     * 업무 공유 추가 로그
     */
    @Transactional
    public void logItemShared(Long itemId, String actorUsername, String targetUsername, String permission) {
        log(AuditLog.TARGET_ITEM_SHARE, itemId, AuditLog.ACTION_SHARE,
                actorUsername, "업무 공유 추가 (" + permission + ")", null, null, targetUsername);
    }

    /**
     * 업무 공유 해제 로그
     */
    @Transactional
    public void logItemUnshared(Long itemId, String actorUsername, String targetUsername) {
        log(AuditLog.TARGET_ITEM_SHARE, itemId, AuditLog.ACTION_UNSHARE,
                actorUsername, "업무 공유 해제", null, null, targetUsername);
    }

    /**
     * 검색 조건으로 로그 조회
     */
    @Transactional(readOnly = true)
    public AuditLogPageResponse search(AuditLogSearchRequest request) {
        List<AuditLog> logs = auditLogMapper.selectBySearchRequest(request);
        long totalCount = auditLogMapper.countBySearchRequest(request);

        List<AuditLogResponse> content = logs.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        int totalPages = (int) Math.ceil((double) totalCount / request.getSize());

        return AuditLogPageResponse.builder()
                .content(content)
                .totalElements(totalCount)
                .totalPages(totalPages)
                .number(request.getPage())
                .size(request.getSize())
                .build();
    }

    /**
     * 보드별 이력 조회
     */
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getByBoard(Long boardId) {
        return auditLogMapper.selectByBoardId(boardId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 업무별 이력 조회
     */
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getByItem(Long itemId) {
        return auditLogMapper.selectByItemId(itemId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 최근 이력 조회
     */
    @Transactional(readOnly = true)
    public List<AuditLogResponse> getRecent(int limit) {
        return auditLogMapper.selectRecent(limit).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .logId(log.getLogId())
                .targetType(log.getTargetType())
                .targetId(log.getTargetId())
                .targetName(log.getTargetName())
                .action(log.getAction())
                .actorUsername(log.getActorUsername())
                .actorName(log.getActorName())
                .description(log.getDescription())
                .relatedUsername(log.getRelatedUsername())
                .relatedUserName(log.getRelatedUserName())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
