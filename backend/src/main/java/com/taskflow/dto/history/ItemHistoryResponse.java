package com.taskflow.dto.history;

import com.taskflow.domain.ItemHistory;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 작업 처리 이력 응답 DTO
 */
@Getter
@Builder
public class ItemHistoryResponse {

    /**
     * 아이템 ID
     */
    private Long itemId;

    /**
     * 보드 ID
     */
    private Long boardId;

    /**
     * 보드명
     */
    private String boardName;

    /**
     * 작업 내용
     */
    private String title;

    /**
     * 작업 결과 (완료/삭제)
     */
    private String result;

    /**
     * 작업자 ID
     */
    private Long workerId;

    /**
     * 작업자 이름
     */
    private String workerName;

    /**
     * 등록시간
     */
    private LocalDateTime createdAt;

    /**
     * 등록자 이름
     */
    private String createdByName;

    /**
     * 시작시간
     */
    private LocalDateTime startTime;

    /**
     * 완료시간
     */
    private LocalDateTime completedAt;

    /**
     * 수정시간
     */
    private LocalDateTime updatedAt;

    /**
     * 삭제시간
     */
    private LocalDateTime deletedAt;

    /**
     * ItemHistory를 ItemHistoryResponse로 변환
     */
    public static ItemHistoryResponse from(ItemHistory history) {
        return ItemHistoryResponse.builder()
                .itemId(history.getItemId())
                .boardId(history.getBoardId())
                .boardName(history.getBoardName())
                .title(history.getTitle())
                .result(history.getResult())
                .workerId(history.getWorkerId())
                .workerName(history.getWorkerName())
                .createdAt(history.getCreatedAt())
                .createdByName(history.getCreatedByName())
                .startTime(history.getStartTime())
                .completedAt(history.getCompletedAt())
                .updatedAt(history.getUpdatedAt())
                .deletedAt(history.getDeletedAt())
                .build();
    }

    /**
     * ItemHistory 리스트를 ItemHistoryResponse 리스트로 변환
     */
    public static List<ItemHistoryResponse> fromList(List<ItemHistory> histories) {
        return histories.stream()
                .map(ItemHistoryResponse::from)
                .collect(Collectors.toList());
    }
}
