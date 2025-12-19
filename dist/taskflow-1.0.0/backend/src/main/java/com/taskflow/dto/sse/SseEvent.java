package com.taskflow.dto.sse;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * SSE 이벤트 DTO
 */
@Getter
@Builder
public class SseEvent<T> {

    // =============================================
    // 이벤트 타입 상수
    // =============================================

    public static final String ITEM_CREATED = "item:created";
    public static final String ITEM_UPDATED = "item:updated";
    public static final String ITEM_DELETED = "item:deleted";
    public static final String PROPERTY_UPDATED = "property:updated";
    public static final String COMMENT_CREATED = "comment:created";
    public static final String CONNECTION = "connection";
    public static final String HEARTBEAT = "heartbeat";

    // =============================================
    // 필드
    // =============================================

    /**
     * 이벤트 타입
     */
    private String type;

    /**
     * 보드 ID (해당 보드를 구독 중인 클라이언트에게만 전송)
     */
    private Long boardId;

    /**
     * 이벤트 데이터
     */
    private T data;

    /**
     * 이벤트 발생 시간
     */
    private LocalDateTime timestamp;

    /**
     * 이벤트 발생자 ID (본인에게는 전송하지 않음)
     */
    private Long triggeredBy;

    // =============================================
    // 팩토리 메서드
    // =============================================

    /**
     * 아이템 생성 이벤트
     */
    public static <T> SseEvent<T> itemCreated(Long boardId, T data, Long triggeredBy) {
        return SseEvent.<T>builder()
                .type(ITEM_CREATED)
                .boardId(boardId)
                .data(data)
                .timestamp(LocalDateTime.now())
                .triggeredBy(triggeredBy)
                .build();
    }

    /**
     * 아이템 수정 이벤트
     */
    public static <T> SseEvent<T> itemUpdated(Long boardId, T data, Long triggeredBy) {
        return SseEvent.<T>builder()
                .type(ITEM_UPDATED)
                .boardId(boardId)
                .data(data)
                .timestamp(LocalDateTime.now())
                .triggeredBy(triggeredBy)
                .build();
    }

    /**
     * 아이템 삭제 이벤트
     */
    public static <T> SseEvent<T> itemDeleted(Long boardId, T data, Long triggeredBy) {
        return SseEvent.<T>builder()
                .type(ITEM_DELETED)
                .boardId(boardId)
                .data(data)
                .timestamp(LocalDateTime.now())
                .triggeredBy(triggeredBy)
                .build();
    }

    /**
     * 속성 정의 변경 이벤트
     */
    public static <T> SseEvent<T> propertyUpdated(Long boardId, T data, Long triggeredBy) {
        return SseEvent.<T>builder()
                .type(PROPERTY_UPDATED)
                .boardId(boardId)
                .data(data)
                .timestamp(LocalDateTime.now())
                .triggeredBy(triggeredBy)
                .build();
    }

    /**
     * 댓글 생성 이벤트
     */
    public static <T> SseEvent<T> commentCreated(Long boardId, T data, Long triggeredBy) {
        return SseEvent.<T>builder()
                .type(COMMENT_CREATED)
                .boardId(boardId)
                .data(data)
                .timestamp(LocalDateTime.now())
                .triggeredBy(triggeredBy)
                .build();
    }

    /**
     * 연결 완료 이벤트
     */
    public static SseEvent<String> connected() {
        return SseEvent.<String>builder()
                .type(CONNECTION)
                .data("connected")
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 하트비트 이벤트
     */
    public static SseEvent<String> heartbeat() {
        return SseEvent.<String>builder()
                .type(HEARTBEAT)
                .data("ping")
                .timestamp(LocalDateTime.now())
                .build();
    }
}
