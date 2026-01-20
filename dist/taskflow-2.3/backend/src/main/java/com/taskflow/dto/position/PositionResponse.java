package com.taskflow.dto.position;

import com.taskflow.domain.Position;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 직급 응답 DTO
 */
@Getter
@Builder
public class PositionResponse {

    /**
     * 직급 ID
     */
    private Long positionId;

    /**
     * 직급 코드
     */
    private String positionCode;

    /**
     * 직급명
     */
    private String positionName;

    /**
     * 정렬 순서 (낮을수록 높은 직급)
     */
    private Integer sortOrder;

    /**
     * 사용 여부
     */
    private String useYn;

    /**
     * 마지막 동기화 시간 (External 모드)
     */
    private LocalDateTime lastSyncedAt;

    /**
     * 생성일시
     */
    private LocalDateTime createdAt;

    /**
     * 생성자
     */
    private String createdBy;

    /**
     * Position 엔티티에서 PositionResponse 생성
     */
    public static PositionResponse from(Position position) {
        if (position == null) {
            return null;
        }

        return PositionResponse.builder()
                .positionId(position.getPositionId())
                .positionCode(position.getPositionCode())
                .positionName(position.getPositionName())
                .sortOrder(position.getSortOrder())
                .useYn(position.getUseYn())
                .lastSyncedAt(position.getLastSyncedAt())
                .createdAt(position.getCreatedAt())
                .createdBy(position.getCreatedBy())
                .build();
    }

    /**
     * Position 목록에서 PositionResponse 목록 생성
     */
    public static List<PositionResponse> fromList(List<Position> positions) {
        return positions.stream()
                .map(PositionResponse::from)
                .collect(Collectors.toList());
    }
}
