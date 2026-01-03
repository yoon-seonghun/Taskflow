package com.taskflow.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 직급 엔티티
 *
 * 테이블: TB_POSITION
 *
 * POSITION_CODE 기반 FK 참조 시스템:
 * - POSITION_ID: 내부 식별자
 * - POSITION_CODE: FK 참조 키 (UNIQUE)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Position {

    /**
     * 직급 ID (PK) - 내부 식별자
     */
    private Long positionId;

    /**
     * 직급 코드 (UNIQUE) - FK 참조 키
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
     * 사용 여부 (Y/N)
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
     * 생성자 USERNAME
     */
    private String createdBy;

    /**
     * 수정일시
     */
    private LocalDateTime updatedAt;

    /**
     * 수정자 USERNAME
     */
    private String updatedBy;

    // =============================================
    // 편의 메서드
    // =============================================

    /**
     * 활성 직급 여부
     */
    public boolean isActive() {
        return "Y".equals(useYn);
    }

    /**
     * 외부 동기화된 직급 여부
     */
    public boolean isSynced() {
        return lastSyncedAt != null;
    }
}
