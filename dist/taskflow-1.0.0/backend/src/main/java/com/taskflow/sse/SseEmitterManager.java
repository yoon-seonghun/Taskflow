package com.taskflow.sse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskflow.dto.sse.SseEvent;
import com.taskflow.dto.sse.SseSubscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE Emitter 관리자
 *
 * SSE 연결을 관리하고 이벤트를 전송합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SseEmitterManager {

    /**
     * SSE 연결 타임아웃 (30분)
     */
    private static final long SSE_TIMEOUT = 30 * 60 * 1000L;

    /**
     * 하트비트 간격 (30초)
     */
    private static final long HEARTBEAT_INTERVAL = 30 * 1000L;

    /**
     * 사용자별 SSE 구독 정보
     * Key: userId
     */
    private final Map<Long, SseSubscription> subscriptions = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper;

    // =============================================
    // 연결 관리
    // =============================================

    /**
     * SSE 연결 생성
     *
     * @param userId 사용자 ID
     * @return SseEmitter
     */
    public SseEmitter createEmitter(Long userId) {
        log.info("Creating SSE emitter for user: {}", userId);

        // 기존 연결이 있으면 종료
        removeEmitter(userId);

        // 새 Emitter 생성
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        // 구독 정보 생성
        SseSubscription subscription = SseSubscription.builder()
                .userId(userId)
                .emitter(emitter)
                .subscribedAt(LocalDateTime.now())
                .lastActiveAt(LocalDateTime.now())
                .build();

        subscriptions.put(userId, subscription);

        // 연결 완료 콜백
        emitter.onCompletion(() -> {
            log.info("SSE connection completed for user: {}", userId);
            removeEmitter(userId);
        });

        // 타임아웃 콜백
        emitter.onTimeout(() -> {
            log.info("SSE connection timed out for user: {}", userId);
            removeEmitter(userId);
        });

        // 에러 콜백
        emitter.onError(throwable -> {
            log.error("SSE connection error for user: {}", userId, throwable);
            removeEmitter(userId);
        });

        // 연결 완료 이벤트 전송
        sendToUser(userId, SseEvent.connected());

        log.info("SSE emitter created for user: {}, total connections: {}", userId, subscriptions.size());

        return emitter;
    }

    /**
     * SSE 연결 제거
     *
     * @param userId 사용자 ID
     */
    public void removeEmitter(Long userId) {
        SseSubscription subscription = subscriptions.remove(userId);
        if (subscription != null) {
            try {
                subscription.getEmitter().complete();
            } catch (Exception e) {
                log.debug("Error completing emitter for user: {}", userId);
            }
            log.info("SSE emitter removed for user: {}, total connections: {}", userId, subscriptions.size());
        }
    }

    // =============================================
    // 보드 구독 관리
    // =============================================

    /**
     * 보드 구독
     *
     * @param userId  사용자 ID
     * @param boardId 보드 ID
     */
    public void subscribeBoard(Long userId, Long boardId) {
        SseSubscription subscription = subscriptions.get(userId);
        if (subscription != null) {
            subscription.subscribeBoard(boardId);
            log.debug("User {} subscribed to board {}", userId, boardId);
        }
    }

    /**
     * 보드 구독 해제
     *
     * @param userId  사용자 ID
     * @param boardId 보드 ID
     */
    public void unsubscribeBoard(Long userId, Long boardId) {
        SseSubscription subscription = subscriptions.get(userId);
        if (subscription != null) {
            subscription.unsubscribeBoard(boardId);
            log.debug("User {} unsubscribed from board {}", userId, boardId);
        }
    }

    // =============================================
    // 이벤트 전송
    // =============================================

    /**
     * 특정 사용자에게 이벤트 전송
     *
     * @param userId 사용자 ID
     * @param event  이벤트
     */
    public <T> void sendToUser(Long userId, SseEvent<T> event) {
        SseSubscription subscription = subscriptions.get(userId);
        if (subscription == null) {
            return;
        }

        try {
            String jsonData = objectMapper.writeValueAsString(event);
            subscription.getEmitter().send(
                    SseEmitter.event()
                            .name(event.getType())
                            .data(jsonData)
            );
            subscription.updateLastActiveAt();
            log.debug("Sent event {} to user {}", event.getType(), userId);
        } catch (IOException e) {
            log.error("Failed to send event to user: {}", userId, e);
            removeEmitter(userId);
        }
    }

    /**
     * 보드 구독자들에게 이벤트 전송 (이벤트 발생자 제외)
     *
     * @param event 이벤트 (boardId, triggeredBy 포함)
     */
    public <T> void sendToBoard(SseEvent<T> event) {
        if (event.getBoardId() == null) {
            log.warn("Board ID is null, cannot send event");
            return;
        }

        subscriptions.values().stream()
                .filter(sub -> sub.isSubscribedTo(event.getBoardId()))
                .filter(sub -> !sub.getUserId().equals(event.getTriggeredBy())) // 이벤트 발생자 제외
                .forEach(sub -> {
                    try {
                        String jsonData = objectMapper.writeValueAsString(event);
                        sub.getEmitter().send(
                                SseEmitter.event()
                                        .name(event.getType())
                                        .data(jsonData)
                        );
                        sub.updateLastActiveAt();
                        log.debug("Sent event {} to user {} for board {}",
                                event.getType(), sub.getUserId(), event.getBoardId());
                    } catch (IOException e) {
                        log.error("Failed to send event to user: {}", sub.getUserId(), e);
                        removeEmitter(sub.getUserId());
                    }
                });
    }

    /**
     * 모든 연결된 사용자에게 이벤트 전송
     *
     * @param event 이벤트
     */
    public <T> void sendToAll(SseEvent<T> event) {
        subscriptions.values().forEach(sub -> {
            // 이벤트 발생자 제외
            if (event.getTriggeredBy() != null && event.getTriggeredBy().equals(sub.getUserId())) {
                return;
            }

            try {
                String jsonData = objectMapper.writeValueAsString(event);
                sub.getEmitter().send(
                        SseEmitter.event()
                                .name(event.getType())
                                .data(jsonData)
                );
                sub.updateLastActiveAt();
            } catch (IOException e) {
                log.error("Failed to send event to user: {}", sub.getUserId(), e);
                removeEmitter(sub.getUserId());
            }
        });
    }

    // =============================================
    // 하트비트
    // =============================================

    /**
     * 주기적 하트비트 전송 (연결 유지)
     */
    @Scheduled(fixedRate = HEARTBEAT_INTERVAL)
    public void sendHeartbeat() {
        if (subscriptions.isEmpty()) {
            return;
        }

        log.debug("Sending heartbeat to {} connections", subscriptions.size());

        SseEvent<String> heartbeat = SseEvent.heartbeat();
        subscriptions.values().forEach(sub -> {
            try {
                String jsonData = objectMapper.writeValueAsString(heartbeat);
                sub.getEmitter().send(
                        SseEmitter.event()
                                .name(heartbeat.getType())
                                .data(jsonData)
                );
                sub.updateLastActiveAt();
            } catch (IOException e) {
                log.debug("Heartbeat failed for user: {}, removing connection", sub.getUserId());
                removeEmitter(sub.getUserId());
            }
        });
    }

    // =============================================
    // 상태 조회
    // =============================================

    /**
     * 연결된 사용자 수 조회
     */
    public int getConnectionCount() {
        return subscriptions.size();
    }

    /**
     * 사용자 연결 여부 확인
     */
    public boolean isConnected(Long userId) {
        return subscriptions.containsKey(userId);
    }
}
