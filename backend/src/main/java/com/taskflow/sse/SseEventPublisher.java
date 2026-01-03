package com.taskflow.sse;

import com.taskflow.dto.comment.CommentResponse;
import com.taskflow.dto.item.ItemResponse;
import com.taskflow.dto.property.PropertyResponse;
import com.taskflow.dto.sse.SseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * SSE 이벤트 발행 서비스
 *
 * 비동기로 이벤트를 발행하여 메인 트랜잭션에 영향을 주지 않습니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SseEventPublisher {

    private final SseEmitterManager emitterManager;

    // =============================================
    // 아이템 이벤트
    // =============================================

    /**
     * 아이템 생성 이벤트 발행
     *
     * @param boardId     보드 ID
     * @param item        생성된 아이템
     * @param triggeredByUsername 이벤트 발생자 USERNAME
     */
    @Async
    public void publishItemCreated(Long boardId, ItemResponse item, String triggeredByUsername) {
        log.debug("Publishing item:created event for board {}, item {}", boardId, item.getItemId());

        SseEvent<ItemResponse> event = SseEvent.itemCreated(boardId, item, triggeredByUsername);
        emitterManager.sendToBoard(event);
    }

    /**
     * 아이템 수정 이벤트 발행
     *
     * @param boardId     보드 ID
     * @param item        수정된 아이템
     * @param triggeredByUsername 이벤트 발생자 USERNAME
     */
    @Async
    public void publishItemUpdated(Long boardId, ItemResponse item, String triggeredByUsername) {
        log.debug("Publishing item:updated event for board {}, item {}", boardId, item.getItemId());

        SseEvent<ItemResponse> event = SseEvent.itemUpdated(boardId, item, triggeredByUsername);
        emitterManager.sendToBoard(event);
    }

    /**
     * 아이템 삭제 이벤트 발행
     *
     * @param boardId     보드 ID
     * @param item        삭제된 아이템
     * @param triggeredByUsername 이벤트 발생자 USERNAME
     */
    @Async
    public void publishItemDeleted(Long boardId, ItemResponse item, String triggeredByUsername) {
        log.debug("Publishing item:deleted event for board {}, item {}", boardId, item.getItemId());

        SseEvent<ItemResponse> event = SseEvent.itemDeleted(boardId, item, triggeredByUsername);
        emitterManager.sendToBoard(event);
    }

    /**
     * 아이템 완료 이벤트 발행 (item:updated로 처리)
     *
     * @param boardId     보드 ID
     * @param item        완료된 아이템
     * @param triggeredByUsername 이벤트 발생자 USERNAME
     */
    @Async
    public void publishItemCompleted(Long boardId, ItemResponse item, String triggeredByUsername) {
        log.debug("Publishing item:updated (completed) event for board {}, item {}", boardId, item.getItemId());

        SseEvent<ItemResponse> event = SseEvent.itemUpdated(boardId, item, triggeredByUsername);
        emitterManager.sendToBoard(event);
    }

    /**
     * 아이템 복원 이벤트 발행 (item:updated로 처리)
     *
     * @param boardId     보드 ID
     * @param item        복원된 아이템
     * @param triggeredByUsername 이벤트 발생자 USERNAME
     */
    @Async
    public void publishItemRestored(Long boardId, ItemResponse item, String triggeredByUsername) {
        log.debug("Publishing item:updated (restored) event for board {}, item {}", boardId, item.getItemId());

        SseEvent<ItemResponse> event = SseEvent.itemUpdated(boardId, item, triggeredByUsername);
        emitterManager.sendToBoard(event);
    }

    // =============================================
    // 속성 이벤트
    // =============================================

    /**
     * 속성 정의 변경 이벤트 발행
     *
     * @param boardId     보드 ID
     * @param property    변경된 속성
     * @param triggeredByUsername 이벤트 발생자 USERNAME
     */
    @Async
    public void publishPropertyUpdated(Long boardId, PropertyResponse property, String triggeredByUsername) {
        log.debug("Publishing property:updated event for board {}, property {}", boardId, property.getPropertyId());

        SseEvent<PropertyResponse> event = SseEvent.propertyUpdated(boardId, property, triggeredByUsername);
        emitterManager.sendToBoard(event);
    }

    /**
     * 속성 정의 생성 이벤트 발행 (property:updated로 처리)
     *
     * @param boardId     보드 ID
     * @param property    생성된 속성
     * @param triggeredByUsername 이벤트 발생자 USERNAME
     */
    @Async
    public void publishPropertyCreated(Long boardId, PropertyResponse property, String triggeredByUsername) {
        log.debug("Publishing property:updated (created) event for board {}, property {}", boardId, property.getPropertyId());

        SseEvent<PropertyResponse> event = SseEvent.propertyUpdated(boardId, property, triggeredByUsername);
        emitterManager.sendToBoard(event);
    }

    /**
     * 속성 정의 삭제 이벤트 발행 (property:updated로 처리)
     *
     * @param boardId     보드 ID
     * @param propertyId  삭제된 속성 ID
     * @param triggeredByUsername 이벤트 발생자 USERNAME
     */
    @Async
    public void publishPropertyDeleted(Long boardId, Long propertyId, String triggeredByUsername) {
        log.debug("Publishing property:updated (deleted) event for board {}, property {}", boardId, propertyId);

        // 삭제된 속성 정보 (ID만 포함)
        PropertyResponse deletedProperty = PropertyResponse.builder()
                .propertyId(propertyId)
                .boardId(boardId)
                .deleted(true)
                .build();

        SseEvent<PropertyResponse> event = SseEvent.propertyUpdated(boardId, deletedProperty, triggeredByUsername);
        emitterManager.sendToBoard(event);
    }

    // =============================================
    // 댓글 이벤트
    // =============================================

    /**
     * 댓글 생성 이벤트 발행
     *
     * @param boardId     보드 ID
     * @param comment     생성된 댓글
     * @param triggeredByUsername 이벤트 발생자 USERNAME
     */
    @Async
    public void publishCommentCreated(Long boardId, CommentResponse comment, String triggeredByUsername) {
        log.debug("Publishing comment:created event for board {}, comment {}", boardId, comment.getCommentId());

        SseEvent<CommentResponse> event = SseEvent.commentCreated(boardId, comment, triggeredByUsername);
        emitterManager.sendToBoard(event);
    }
}
