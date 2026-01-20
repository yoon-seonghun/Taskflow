package com.taskflow.service.impl;

import com.taskflow.config.UserManagementProperties;
import com.taskflow.domain.Position;
import com.taskflow.dto.position.*;
import com.taskflow.exception.BusinessException;
import com.taskflow.mapper.PositionMapper;
import com.taskflow.service.PositionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 직급 서비스 구현 (Internal 모드)
 *
 * 기본 관리 모드로 TaskFlow DB에서 직급 데이터를 직접 관리
 * - 직급 CRUD 가능
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@ConditionalOnProperty(
    name = "taskflow.user-management.mode",
    havingValue = "internal",
    matchIfMissing = true
)
public class PositionServiceImpl implements PositionService {

    private final PositionMapper positionMapper;
    private final UserManagementProperties userManagementProperties;

    // =============================================
    // 모드 확인
    // =============================================

    @Override
    public boolean isCrudEnabled() {
        return userManagementProperties.isPositionCrudEnabled();
    }

    // =============================================
    // 조회
    // =============================================

    @Override
    public PositionResponse getPosition(String positionCode) {
        Position position = positionMapper.findByCode(positionCode)
                .orElseThrow(() -> BusinessException.notFound("직급", positionCode));
        return PositionResponse.from(position);
    }

    @Override
    public List<PositionResponse> getPositions(String useYn) {
        List<Position> positions = positionMapper.findAll(useYn);
        return PositionResponse.fromList(positions);
    }

    @Override
    public boolean existsByCode(String positionCode) {
        return positionMapper.existsByCode(positionCode);
    }

    // =============================================
    // 등록/수정/삭제
    // =============================================

    @Override
    @Transactional
    public PositionResponse createPosition(PositionCreateRequest request, String createdBy) {
        log.info("Creating position: code={}", request.getPositionCode());

        // CRUD 활성화 확인
        if (!isCrudEnabled()) {
            throw BusinessException.forbidden("External 모드에서는 직급 생성이 불가합니다.");
        }

        // 직급 코드 중복 확인
        if (positionMapper.existsByCode(request.getPositionCode())) {
            throw BusinessException.duplicateCode(request.getPositionCode());
        }

        // 정렬 순서 설정 및 기존 순서 조정
        Integer sortOrder = request.getSortOrder();
        if (sortOrder == null) {
            // 순서 미지정 시 마지막에 추가
            sortOrder = positionMapper.getMaxSortOrder() + 1;
        } else {
            // 순서 지정 시, 해당 순서 이상의 기존 직급들 순서를 1씩 증가
            if (positionMapper.existsBySortOrder(sortOrder)) {
                positionMapper.incrementSortOrdersFrom(sortOrder, createdBy);
                log.info("Shifted sort orders from {} for new position", sortOrder);
            }
        }

        // 직급 엔티티 생성
        Position position = Position.builder()
                .positionCode(request.getPositionCode())
                .positionName(request.getPositionName())
                .sortOrder(sortOrder)
                .useYn("Y")
                .createdBy(createdBy)
                .build();

        // 저장
        positionMapper.insert(position);
        log.info("Position created: code={}", position.getPositionCode());

        return getPosition(position.getPositionCode());
    }

    @Override
    @Transactional
    public PositionResponse updatePosition(String positionCode, PositionUpdateRequest request, String updatedBy) {
        log.info("Updating position: code={}", positionCode);

        // CRUD 활성화 확인
        if (!isCrudEnabled()) {
            throw BusinessException.forbidden("External 모드에서는 직급 수정이 불가합니다.");
        }

        // 직급 존재 확인
        Position position = positionMapper.findByCode(positionCode)
                .orElseThrow(() -> BusinessException.notFound("직급", positionCode));

        // 수정
        position.setPositionName(request.getPositionName());
        if (request.getSortOrder() != null) {
            position.setSortOrder(request.getSortOrder());
        }
        if (request.getUseYn() != null) {
            position.setUseYn(request.getUseYn());
        }
        position.setUpdatedBy(updatedBy);

        positionMapper.update(position);
        log.info("Position updated: code={}", positionCode);

        return getPosition(positionCode);
    }

    @Override
    @Transactional
    public void updatePositionOrder(PositionOrderRequest request, String updatedBy) {
        log.info("Updating position order: codes={}", request.getPositionCodes());

        // CRUD 활성화 확인
        if (!isCrudEnabled()) {
            throw BusinessException.forbidden("External 모드에서는 직급 순서 변경이 불가합니다.");
        }

        // 순서 변경
        List<String> codes = request.getPositionCodes();
        for (int i = 0; i < codes.size(); i++) {
            positionMapper.updateSortOrder(codes.get(i), i + 1, updatedBy);
        }

        log.info("Position order updated");
    }

    @Override
    @Transactional
    public void deletePosition(String positionCode) {
        log.info("Deleting position: code={}", positionCode);

        // CRUD 활성화 확인
        if (!isCrudEnabled()) {
            throw BusinessException.forbidden("External 모드에서는 직급 삭제가 불가합니다.");
        }

        // 직급 존재 확인
        positionMapper.findByCode(positionCode)
                .orElseThrow(() -> BusinessException.notFound("직급", positionCode));

        // 사용 중인 사용자 확인
        int userCount = positionMapper.countUsersByPositionCode(positionCode);
        if (userCount > 0) {
            throw BusinessException.dataInUse(
                String.format("해당 직급을 사용 중인 사용자가 %d명 있어 삭제할 수 없습니다. 사용자의 직급을 먼저 변경해주세요.", userCount)
            );
        }

        // 삭제
        positionMapper.delete(positionCode);
        log.info("Position deleted: code={}", positionCode);
    }
}
