package com.taskflow.service.impl;

import com.taskflow.domain.Position;
import com.taskflow.dto.position.*;
import com.taskflow.exception.BusinessException;
import com.taskflow.mapper.external.ExternalPositionMapper;
import com.taskflow.service.PositionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 직급 서비스 구현 (External 모드)
 *
 * 외부 DB 연동 모드로 읽기 전용
 * - 직급 조회: 외부 DB에서 조회 (VIEW)
 * - CRUD 불가 (외부 시스템에서 관리)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@ConditionalOnProperty(
    name = "taskflow.user-management.mode",
    havingValue = "external"
)
public class ExternalPositionServiceImpl implements PositionService {

    private final ExternalPositionMapper externalPositionMapper;

    // =============================================
    // 모드 확인
    // =============================================

    @Override
    public boolean isCrudEnabled() {
        return false; // 외부 모드는 항상 읽기 전용
    }

    // =============================================
    // 조회
    // =============================================

    @Override
    public PositionResponse getPosition(String positionCode) {
        Position position = externalPositionMapper.findByCode(positionCode)
                .orElseThrow(() -> BusinessException.notFound("직급을 찾을 수 없습니다: " + positionCode));
        return PositionResponse.from(position);
    }

    @Override
    public List<PositionResponse> getPositions(String useYn) {
        return externalPositionMapper.findAll(useYn).stream()
                .map(PositionResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByCode(String positionCode) {
        return externalPositionMapper.existsByCode(positionCode);
    }

    // =============================================
    // 등록/수정/삭제 (읽기 전용으로 모두 예외 발생)
    // =============================================

    @Override
    public PositionResponse createPosition(PositionCreateRequest request, String createdBy) {
        throw BusinessException.operationNotAllowed("외부 DB 연동 모드에서는 직급 등록이 불가능합니다.");
    }

    @Override
    public PositionResponse updatePosition(String positionCode, PositionUpdateRequest request, String updatedBy) {
        throw BusinessException.operationNotAllowed("외부 DB 연동 모드에서는 직급 수정이 불가능합니다.");
    }

    @Override
    public void updatePositionOrder(PositionOrderRequest request, String updatedBy) {
        throw BusinessException.operationNotAllowed("외부 DB 연동 모드에서는 직급 순서 변경이 불가능합니다.");
    }

    @Override
    public void deletePosition(String positionCode) {
        throw BusinessException.operationNotAllowed("외부 DB 연동 모드에서는 직급 삭제가 불가능합니다.");
    }
}
