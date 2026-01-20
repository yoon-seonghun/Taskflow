package com.taskflow.service.impl;

import com.taskflow.domain.Position;
import com.taskflow.dto.sync.SyncResult;
import com.taskflow.mapper.PositionMapper;
import com.taskflow.mapper.external.ExternalPositionMapper;
import com.taskflow.service.PositionSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 직급 동기화 서비스 구현
 *
 * External 모드에서만 활성화
 *
 * POSITION_CODE 기반 FK 참조 시스템:
 * - POSITION_CODE 기준 UPSERT 방식
 * - 외부/내부 ID 매핑 불필요 (POSITION_CODE가 자연키)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "taskflow.user-management.mode",
    havingValue = "external"
)
public class PositionSyncServiceImpl implements PositionSyncService {

    private final PositionMapper positionMapper;
    private final ExternalPositionMapper externalPositionMapper;

    @Override
    @Transactional
    public boolean syncPositionByCode(String positionCode) {
        if (positionCode == null || positionCode.isBlank()) {
            return false;
        }

        log.debug("Syncing position by code: {}", positionCode);

        // 1. 외부 DB에서 직급 정보 조회
        Optional<Position> externalPositionOpt = externalPositionMapper.findByCode(positionCode);
        if (externalPositionOpt.isEmpty()) {
            log.warn("External position not found: code={}", positionCode);
            return false;
        }

        Position externalPosition = externalPositionOpt.get();

        // 2. UPSERT (POSITION_CODE 기준)
        Position position = Position.builder()
                .positionCode(externalPosition.getPositionCode())
                .positionName(externalPosition.getPositionName())
                .sortOrder(externalPosition.getSortOrder() != null ? externalPosition.getSortOrder() : 0)
                .useYn(externalPosition.getUseYn() != null ? externalPosition.getUseYn() : "Y")
                .createdBy("SYSTEM")
                .build();

        positionMapper.upsertByCode(position);
        log.debug("Position synced: code={}, name={}", positionCode, externalPosition.getPositionName());

        return true;
    }

    @Override
    @Transactional
    public SyncResult syncAllPositions() {
        LocalDateTime startedAt = LocalDateTime.now();
        log.info("Starting full position synchronization (UPSERT mode)...");

        int created = 0;
        int updated = 0;
        int deactivated = 0;

        try {
            // 1. 외부 DB에서 모든 활성 직급 조회
            List<Position> externalPositions = externalPositionMapper.findAllActive();
            log.info("Found {} active positions in external DB", externalPositions.size());

            if (externalPositions.isEmpty()) {
                return SyncResult.success("POSITION", 0, 0, 0, 0, startedAt);
            }

            // 2. 각 직급 UPSERT
            for (Position externalPosition : externalPositions) {
                // 기존 데이터 존재 여부 확인 (통계용)
                boolean exists = positionMapper.existsByCode(externalPosition.getPositionCode());

                Position position = Position.builder()
                        .positionCode(externalPosition.getPositionCode())
                        .positionName(externalPosition.getPositionName())
                        .sortOrder(externalPosition.getSortOrder() != null ? externalPosition.getSortOrder() : 0)
                        .useYn(externalPosition.getUseYn() != null ? externalPosition.getUseYn() : "Y")
                        .createdBy("SYSTEM")
                        .build();

                positionMapper.upsertByCode(position);

                if (exists) {
                    updated++;
                } else {
                    created++;
                }

                log.debug("Synced position: {} ({})", externalPosition.getPositionCode(), externalPosition.getPositionName());
            }

            // 3. 동기화 대상이 아닌 직급 비활성화
            List<String> activeCodes = externalPositions.stream()
                    .map(Position::getPositionCode)
                    .collect(Collectors.toList());

            if (!activeCodes.isEmpty()) {
                deactivated = positionMapper.deactivatePositionsNotIn(activeCodes);
                if (deactivated > 0) {
                    log.info("Deactivated {} positions not in external DB", deactivated);
                }
            }

            log.info("Position sync completed: created={}, updated={}, deactivated={}",
                    created, updated, deactivated);

            return SyncResult.success("POSITION", created, updated, deactivated, 0, startedAt);

        } catch (Exception e) {
            log.error("Position sync failed", e);
            return SyncResult.failure("POSITION", e.getMessage(), startedAt);
        }
    }
}
