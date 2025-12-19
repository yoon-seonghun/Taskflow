package com.taskflow.dto.sse;

import lombok.Builder;
import lombok.Getter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE 구독 정보
 */
@Getter
@Builder
public class SseSubscription {

    /**
     * 사용자 ID
     */
    private Long userId;

    /**
     * SSE Emitter
     */
    private SseEmitter emitter;

    /**
     * 구독 중인 보드 ID 목록
     */
    @Builder.Default
    private Set<Long> subscribedBoardIds = ConcurrentHashMap.newKeySet();

    /**
     * 구독 시작 시간
     */
    private LocalDateTime subscribedAt;

    /**
     * 마지막 활동 시간
     */
    private LocalDateTime lastActiveAt;

    /**
     * 보드 구독 추가
     */
    public void subscribeBoard(Long boardId) {
        subscribedBoardIds.add(boardId);
    }

    /**
     * 보드 구독 해제
     */
    public void unsubscribeBoard(Long boardId) {
        subscribedBoardIds.remove(boardId);
    }

    /**
     * 보드 구독 여부 확인
     */
    public boolean isSubscribedTo(Long boardId) {
        return subscribedBoardIds.contains(boardId);
    }

    /**
     * 활동 시간 갱신
     */
    public void updateLastActiveAt() {
        this.lastActiveAt = LocalDateTime.now();
    }
}
