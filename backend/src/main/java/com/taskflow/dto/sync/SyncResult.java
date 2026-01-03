package com.taskflow.dto.sync;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 동기화 결과 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SyncResult {

    /**
     * 생성된 레코드 수
     */
    private int created;

    /**
     * 갱신된 레코드 수
     */
    private int updated;

    /**
     * 비활성화된 레코드 수
     */
    private int deactivated;

    /**
     * 스킵된 레코드 수 (변경 없음)
     */
    private int skipped;

    /**
     * 실패한 레코드 수
     */
    private int failed;

    /**
     * 동기화 시작 시간
     */
    private LocalDateTime startedAt;

    /**
     * 동기화 완료 시간
     */
    private LocalDateTime completedAt;

    /**
     * 성공 여부
     */
    private boolean success;

    /**
     * 에러 메시지 (실패 시)
     */
    private String errorMessage;

    /**
     * 동기화 유형 (USER/DEPARTMENT)
     */
    private String syncType;

    /**
     * 총 처리 건수
     */
    public int getTotalProcessed() {
        return created + updated + deactivated + skipped;
    }

    /**
     * 빌더 헬퍼 - 성공 결과 생성
     */
    public static SyncResult success(String syncType, int created, int updated, int deactivated, int skipped,
                                     LocalDateTime startedAt) {
        return SyncResult.builder()
                .syncType(syncType)
                .created(created)
                .updated(updated)
                .deactivated(deactivated)
                .skipped(skipped)
                .failed(0)
                .startedAt(startedAt)
                .completedAt(LocalDateTime.now())
                .success(true)
                .build();
    }

    /**
     * 빌더 헬퍼 - 실패 결과 생성
     */
    public static SyncResult failure(String syncType, String errorMessage, LocalDateTime startedAt) {
        return SyncResult.builder()
                .syncType(syncType)
                .created(0)
                .updated(0)
                .deactivated(0)
                .skipped(0)
                .failed(0)
                .startedAt(startedAt)
                .completedAt(LocalDateTime.now())
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}
